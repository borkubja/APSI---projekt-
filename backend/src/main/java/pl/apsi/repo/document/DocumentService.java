package pl.apsi.repo.document;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import pl.apsi.repo.config.ConflictException;
import pl.apsi.repo.config.ForbiddenException;
import pl.apsi.repo.config.NotFoundException;
import pl.apsi.repo.document.DocumentDtos.DocumentDto;
import pl.apsi.repo.document.DocumentDtos.EventDto;
import pl.apsi.repo.document.DocumentDtos.NewDocument;
import pl.apsi.repo.document.DocumentDtos.VersionDto;
import pl.apsi.repo.repository.AccessLevel;
import pl.apsi.repo.repository.ActionStage;
import pl.apsi.repo.repository.ActionType;
import pl.apsi.repo.repository.ActionTypeRepository;
import pl.apsi.repo.repository.Folder;
import pl.apsi.repo.repository.FolderRepository;
import pl.apsi.repo.repository.PermissionService;
import pl.apsi.repo.user.AppUser;
import pl.apsi.repo.user.AppUserRepository;

@Service
public class DocumentService {

    static final String DEFAULT_FIRST_VERSION = "0.1";

    private final DocumentRepository documents;
    private final DocumentVersionRepository versions;
    private final VersionEventRepository events;
    private final FolderRepository folders;
    private final ActionTypeRepository actionTypes;
    private final PermissionService permissions;
    private final AppUserRepository users;
    private final FileStorage storage;

    public DocumentService(DocumentRepository documents, DocumentVersionRepository versions,
                           VersionEventRepository events, FolderRepository folders,
                           ActionTypeRepository actionTypes, PermissionService permissions,
                           AppUserRepository users, FileStorage storage) {
        this.documents = documents;
        this.versions = versions;
        this.events = events;
        this.folders = folders;
        this.actionTypes = actionTypes;
        this.permissions = permissions;
        this.users = users;
        this.storage = storage;
    }

    /** UC2: dokumenty folderu - upoważnienie do odczytu; dokumenty z ograniczoną widocznością tylko dla uprawnionych. */
    @Transactional(readOnly = true)
    public List<DocumentDto> listInFolder(Long folderId, AppUser user) {
        if (!folders.existsById(folderId)) {
            throw new NotFoundException("Folder", folderId);
        }
        AccessLevel level = permissions.require(user.getId(), folderId, AccessLevel.READ);
        return documents.findByFolderIdAndDeletedFalseOrderByTitleAsc(folderId).stream()
            .filter(d -> canSee(d, user, level))
            .map(this::toDto)
            .toList();
    }

    /** Dokument bez pliku (JSON). */
    @Transactional
    public DocumentDto create(Long folderId, NewDocument req, AppUser author) {
        return create(folderId, req, null, author);
    }

    /**
     * UC7: nowy dokument z pierwszą wersją (z plikiem lub bez) i zdarzeniem początkowym
     * obiegu ("Utworzyłem") - wymaga zapisu do folderu. Autor wybiera, kto dokument zobaczy.
     */
    @Transactional
    public DocumentDto create(Long folderId, NewDocument req, MultipartFile file, AppUser author) {
        Folder folder = folders.findById(folderId)
            .orElseThrow(() -> new NotFoundException("Folder", folderId));
        permissions.require(author.getId(), folderId, AccessLevel.WRITE);

        Document doc = new Document(req.title().trim(), req.description(), folder, author);
        Set<Long> viewers = validViewers(req.viewerIds());
        doc.setVisibility(!viewers.isEmpty(), viewers);
        doc = documents.save(doc);

        String label = (req.initialVersion() == null || req.initialVersion().isBlank())
            ? DEFAULT_FIRST_VERSION
            : req.initialVersion();
        DocumentVersion version = new DocumentVersion(doc, null, label, "Utworzenie dokumentu", author);
        attach(version, file);
        version = versions.save(version);

        events.save(new VersionEvent(version, actionOf(folder, ActionStage.START), author, null));
        return toDto(doc);
    }

    /** Zmiana widoczności dokumentu - autor albo osoba z zapisem do folderu. */
    @Transactional
    public DocumentDto setVisibility(Long documentId, boolean restricted, List<Long> viewerIds, AppUser user) {
        Document doc = load(documentId);
        AccessLevel level = permissions.require(user.getId(), doc.getFolder().getId(), AccessLevel.READ);
        if (level != AccessLevel.WRITE && !doc.getAuthor().getId().equals(user.getId())) {
            throw new ForbiddenException("Widoczność dokumentu zmienia autor albo osoba z zapisem do folderu");
        }
        doc.setVisibility(restricted, validViewers(viewerIds));
        return toDto(doc);
    }

    /** Wersje dokumentu z rodzicami i historią obiegu - na tej podstawie frontend rysuje drzewo. */
    @Transactional(readOnly = true)
    public List<VersionDto> versions(Long documentId, AppUser user) {
        Document doc = readable(documentId, user);
        return versions.findByDocumentIdOrderByIdAsc(doc.getId()).stream().map(this::toVersionDto).toList();
    }

    /**
     * Edycja = nowa wersja z nowym plikiem. Rodzicem jest wskazana wersja (domyślnie najnowsza);
     * etykieta nadawana automatycznie, jeśli nie podano własnej. Wymaga zapisu do folderu.
     */
    @Transactional
    public VersionDto addVersion(Long documentId, Long parentVersionId, String label, String changeNote,
                                 MultipartFile file, AppUser user) {
        Document doc = load(documentId);
        permissions.require(user.getId(), doc.getFolder().getId(), AccessLevel.WRITE);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Nowa wersja wymaga pliku");
        }
        DocumentVersion parent = parentVersionId == null
            ? versions.findFirstByDocumentIdOrderByIdDesc(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Dokument nie ma jeszcze wersji"))
            : versions.findById(parentVersionId)
                .filter(v -> v.getDocument().getId().equals(documentId))
                .orElseThrow(() -> new NotFoundException("Wersja", parentVersionId));

        String newLabel = (label == null || label.isBlank()) ? nextLabel(parent, documentId) : label.trim();
        if (!newLabel.matches("^\\d+(\\.\\d+)*$")) {
            throw new IllegalArgumentException("Etykieta wersji w formacie np. 1.0 lub 1.2.1");
        }
        if (versions.existsByDocumentIdAndVersionLabel(documentId, newLabel)) {
            throw new ConflictException("Wersja " + newLabel + " już istnieje");
        }
        String note = (changeNote == null || changeNote.isBlank()) ? "Nowa wersja" : changeNote.trim();
        DocumentVersion version = new DocumentVersion(doc, parent.getId(), newLabel, note, user);
        attach(version, file);
        version = versions.save(version);
        events.save(new VersionEvent(version, actionOf(doc.getFolder(), ActionStage.MIDDLE), user, note));
        return toVersionDto(version);
    }

    /** Zatwierdzenie wersji przez osobę z zapisem do folderu: zdarzenie "Zatwierdziłem" i blokada wersji. */
    @Transactional
    public VersionDto approve(Long versionId, String comment, AppUser user) {
        DocumentVersion version = versions.findById(versionId)
            .orElseThrow(() -> new NotFoundException("Wersja", versionId));
        Folder folder = version.getDocument().getFolder();
        permissions.require(user.getId(), folder.getId(), AccessLevel.WRITE);
        if (version.isApproved()) {
            throw new ConflictException("Wersja " + version.getVersionLabel() + " jest już zatwierdzona");
        }
        version.approve();
        String note = (comment == null || comment.isBlank()) ? null : comment.trim();
        events.save(new VersionEvent(version, actionOf(folder, ActionStage.END), user, note));
        return toVersionDto(version);
    }

    public record FileContent(Resource resource, String fileName, String mimeType) {
    }

    /** Odczyt pliku wersji - upoważnienie do odczytu i widoczność dokumentu. */
    @Transactional(readOnly = true)
    public FileContent file(Long versionId, AppUser user) {
        DocumentVersion version = versions.findById(versionId)
            .orElseThrow(() -> new NotFoundException("Wersja", versionId));
        readable(version.getDocument().getId(), user);
        if (version.getFileKey() == null) {
            throw new NotFoundException("Plik wersji", versionId);
        }
        return new FileContent(storage.load(version.getFileKey()), version.getFileName(), version.getMimeType());
    }

    // ---- pomocnicze ----

    private Document load(Long documentId) {
        return documents.findById(documentId)
            .filter(d -> !d.isDeleted())
            .orElseThrow(() -> new NotFoundException("Dokument", documentId));
    }

    /** Dokument, który użytkownik może czytać (folder + ograniczona widoczność); inaczej 403. */
    private Document readable(Long documentId, AppUser user) {
        Document doc = load(documentId);
        AccessLevel level = permissions.require(user.getId(), doc.getFolder().getId(), AccessLevel.READ);
        if (!canSee(doc, user, level)) {
            throw new ForbiddenException("Ten dokument nie jest dla Ciebie widoczny");
        }
        return doc;
    }

    private boolean canSee(Document d, AppUser user, AccessLevel level) {
        return !d.isRestricted()
            || level == AccessLevel.WRITE
            || d.getAuthor().getId().equals(user.getId())
            || d.getViewerIds().contains(user.getId());
    }

    private Set<Long> validViewers(List<Long> ids) {
        Set<Long> result = new HashSet<>();
        if (ids == null) {
            return result;
        }
        for (Long id : ids) {
            if (!users.existsById(id)) {
                throw new IllegalArgumentException("Nieznany użytkownik: " + id);
            }
            result.add(id);
        }
        return result;
    }

    private void attach(DocumentVersion version, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        FileStorage.Stored stored = storage.save(file);
        String name = file.getOriginalFilename() == null ? "plik" : file.getOriginalFilename();
        String mime = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        version.attachFile(stored.key(), name, stored.sha256(), stored.size(), mime);
    }

    private ActionType actionOf(Folder folder, ActionStage stage) {
        Long repositoryId = folder.getRepository().getId();
        return actionTypes.findFirstByRoleRepositoryIdAndStageOrderBySortOrderAsc(repositoryId, stage)
            .orElseThrow(() -> new IllegalStateException(
                "Słownik działań repozytorium " + repositoryId + " nie ma działania " + stage));
    }

    /** Kolejna etykieta po rodzicu: 1.0 -> 1.1; gdy zajęta (wersja równoległa) -> 1.0.1, 1.0.2... */
    String nextLabel(DocumentVersion parent, Long documentId) {
        String[] parts = parent.getVersionLabel().split("\\.");
        parts[parts.length - 1] = String.valueOf(Long.parseLong(parts[parts.length - 1]) + 1);
        String candidate = String.join(".", parts);
        int branch = 1;
        while (versions.existsByDocumentIdAndVersionLabel(documentId, candidate)) {
            candidate = parent.getVersionLabel() + "." + branch++;
        }
        return candidate;
    }

    private VersionDto toVersionDto(DocumentVersion v) {
        List<EventDto> history = events.findByVersionIdOrderByIdAsc(v.getId()).stream()
            .map(e -> new EventDto(e.getActionType().getName(), e.getActionType().getStage().name(),
                e.getUser().getDisplayName(), e.getComment(), e.getCreatedAt()))
            .toList();
        return new VersionDto(v.getId(), v.getVersionLabel(), v.getParentVersionId(), v.isApproved(),
            v.getChangeNote(), v.getAuthor().getDisplayName(), v.getCreatedAt(),
            v.getFileName(), v.getSizeBytes(), v.getMimeType(), history);
    }

    private DocumentDto toDto(Document d) {
        DocumentVersion latest = versions.findFirstByDocumentIdOrderByIdDesc(d.getId()).orElse(null);
        String lastAction = latest == null ? null
            : events.findFirstByVersionIdOrderByIdDesc(latest.getId())
                .map(e -> e.getActionType().getName())
                .orElse(null);
        return new DocumentDto(d.getId(), d.getTitle(), d.getDescription(), d.getAuthor().getDisplayName(),
            d.getCreatedAt(),
            latest == null ? null : latest.getVersionLabel(),
            latest != null && latest.isApproved(),
            lastAction,
            latest == null ? null : latest.getId(),
            latest == null ? null : latest.getFileName(),
            d.isRestricted(),
            List.copyOf(d.getViewerIds()));
    }
}
