package pl.apsi.repo.document;

import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import pl.apsi.repo.document.DocumentDtos.ApproveRequest;
import pl.apsi.repo.document.DocumentDtos.DocumentDto;
import pl.apsi.repo.document.DocumentDtos.NewDocument;
import pl.apsi.repo.document.DocumentDtos.VersionDto;
import pl.apsi.repo.document.DocumentDtos.VisibilityRequest;
import pl.apsi.repo.user.CurrentUserService;

/** Dostęp sprawdza DocumentService na podstawie upoważnień do folderu (403 przy ich braku). */
@RestController
@RequestMapping("/api")
public class DocumentController {

    private final DocumentService service;
    private final CurrentUserService currentUser;

    public DocumentController(DocumentService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/folders/{folderId}/documents")
    public List<DocumentDto> list(@PathVariable Long folderId, @AuthenticationPrincipal Jwt jwt) {
        return service.listInFolder(folderId, currentUser.fromToken(jwt));
    }

    /** Dokument bez pliku (JSON). */
    @PostMapping(value = "/folders/{folderId}/documents", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto create(@PathVariable Long folderId, @Valid @RequestBody NewDocument req,
                              @AuthenticationPrincipal Jwt jwt) {
        return service.create(folderId, req, currentUser.fromToken(jwt));
    }

    /** Dokument z plikiem (formularz multipart); viewerIds = kto ma go widzieć (puste = wszyscy z dostępem do folderu). */
    @PostMapping(value = "/folders/{folderId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto createWithFile(@PathVariable Long folderId,
                                      @RequestParam String title,
                                      @RequestParam(required = false) String description,
                                      @RequestParam(required = false) String initialVersion,
                                      @RequestParam(required = false) List<Long> viewerIds,
                                      @RequestParam(required = false) MultipartFile file,
                                      @AuthenticationPrincipal Jwt jwt) {
        if (title == null || title.isBlank() || title.length() > 255) {
            throw new IllegalArgumentException("Tytuł jest wymagany (do 255 znaków)");
        }
        if (initialVersion != null && !initialVersion.isBlank() && !initialVersion.matches("^\\d+(\\.\\d+)*$")) {
            throw new IllegalArgumentException("Etykieta wersji w formacie np. 1.0 lub 1.2.1");
        }
        return service.create(folderId, new NewDocument(title, description, initialVersion, viewerIds), file,
            currentUser.fromToken(jwt));
    }

    @PutMapping("/documents/{documentId}/visibility")
    public DocumentDto visibility(@PathVariable Long documentId, @Valid @RequestBody VisibilityRequest req,
                                  @AuthenticationPrincipal Jwt jwt) {
        return service.setVisibility(documentId, req.restricted(), req.viewerIds(), currentUser.fromToken(jwt));
    }

    @GetMapping("/documents/{documentId}/versions")
    public List<VersionDto> versions(@PathVariable Long documentId, @AuthenticationPrincipal Jwt jwt) {
        return service.versions(documentId, currentUser.fromToken(jwt));
    }

    /** Nowa wersja (edycja): plik + opcjonalnie rodzic, etykieta i opis zmiany. */
    @PostMapping(value = "/documents/{documentId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public VersionDto addVersion(@PathVariable Long documentId,
                                 @RequestParam(required = false) Long parentVersionId,
                                 @RequestParam(required = false) String label,
                                 @RequestParam(required = false) String changeNote,
                                 @RequestParam MultipartFile file,
                                 @AuthenticationPrincipal Jwt jwt) {
        return service.addVersion(documentId, parentVersionId, label, changeNote, file, currentUser.fromToken(jwt));
    }

    @PostMapping("/versions/{versionId}/approve")
    public VersionDto approve(@PathVariable Long versionId, @Valid @RequestBody(required = false) ApproveRequest req,
                              @AuthenticationPrincipal Jwt jwt) {
        return service.approve(versionId, req == null ? null : req.comment(), currentUser.fromToken(jwt));
    }

    /** Pobranie pliku wersji; zawsze jako załącznik, żeby przeglądarka nie wykonała treści. */
    @GetMapping("/versions/{versionId}/file")
    public ResponseEntity<org.springframework.core.io.Resource> file(@PathVariable Long versionId,
                                                                      @AuthenticationPrincipal Jwt jwt) {
        DocumentService.FileContent content = service.file(versionId, currentUser.fromToken(jwt));
        MediaType type = MediaType.APPLICATION_OCTET_STREAM;
        try {
            if (content.mimeType() != null) {
                type = MediaType.parseMediaType(content.mimeType());
            }
        } catch (IllegalArgumentException ignored) {
            // nieznany typ MIME - zostaje octet-stream
        }
        return ResponseEntity.ok()
            .contentType(type)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(content.fileName(), StandardCharsets.UTF_8).build().toString())
            .body(content.resource());
    }
}
