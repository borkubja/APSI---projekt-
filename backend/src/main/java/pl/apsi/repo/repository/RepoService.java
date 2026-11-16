package pl.apsi.repo.repository;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pl.apsi.repo.config.ConflictException;
import pl.apsi.repo.config.NotFoundException;
import pl.apsi.repo.repository.RepoDtos.FolderDto;
import pl.apsi.repo.repository.RepoDtos.NewFolder;
import pl.apsi.repo.repository.RepoDtos.NewRepository;
import pl.apsi.repo.repository.RepoDtos.RepositoryDto;
import pl.apsi.repo.user.AppUser;

@Service
public class RepoService {

    /** Domyślny słownik ról i działań nowego repozytorium: rola -> działanie, etap. */
    private record DefaultAction(String role, String action, ActionStage stage) {
    }

    static final List<DefaultAction> DEFAULT_DICTIONARY = List.of(
        new DefaultAction("Autor", "Utworzyłem", ActionStage.START),
        new DefaultAction("Edytor", "Edytowałem", ActionStage.MIDDLE),
        new DefaultAction("Kontroler", "Skontrolowałem", ActionStage.MIDDLE),
        new DefaultAction("Recenzent", "Zrecenzowałem", ActionStage.MIDDLE),
        new DefaultAction("Recenzent", "Zgłosiłem uwagi", ActionStage.MIDDLE),
        new DefaultAction("Zatwierdzający", "Zatwierdziłem", ActionStage.END),
        new DefaultAction("Odbierający", "Odebrałem", ActionStage.AFTER_END));

    private final RepoRepository repos;
    private final FolderRepository folders;
    private final FolderPermissionRepository permissions;
    private final RoleDefinitionRepository roles;
    private final ActionTypeRepository actions;
    private final PermissionService permissionService;

    public RepoService(RepoRepository repos, FolderRepository folders, FolderPermissionRepository permissions,
                       RoleDefinitionRepository roles, ActionTypeRepository actions,
                       PermissionService permissionService) {
        this.repos = repos;
        this.folders = folders;
        this.permissions = permissions;
        this.roles = roles;
        this.actions = actions;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public List<RepositoryDto> accessibleRepositories(AppUser user) {
        return repos.findAccessibleBy(user.getId()).stream()
            .map(r -> new RepositoryDto(r.getId(), r.getName(), r.getDescription(), r.getOwner().getDisplayName()))
            .toList();
    }

    /**
     * UC3: tworzy repozytorium z folderem głównym, nadaje twórcy upoważnienie
     * do zapisu i zakłada domyślny słownik ról i działań.
     */
    @Transactional
    public RepositoryDto createRepository(NewRepository req, AppUser owner) {
        String name = req.name().trim();
        if (repos.existsByName(name)) {
            throw new ConflictException("Repozytorium o nazwie \"" + name + "\" już istnieje");
        }
        Repo repo = repos.save(new Repo(name, req.description(), owner));
        Folder root = folders.save(new Folder(name, repo, null));
        permissions.save(new FolderPermission(root.getId(), owner.getId(), AccessLevel.WRITE, owner.getId()));
        createDefaultDictionary(repo);
        return new RepositoryDto(repo.getId(), repo.getName(), repo.getDescription(), owner.getDisplayName());
    }

    /** Foldery repozytorium widoczne dla użytkownika, z jego poziomem dostępu. */
    @Transactional(readOnly = true)
    public List<FolderDto> folders(Long repositoryId, AppUser user) {
        if (!repos.existsById(repositoryId)) {
            throw new NotFoundException("Repozytorium", repositoryId);
        }
        return folders.findByRepositoryIdOrderByNameAsc(repositoryId).stream()
            .map(f -> new FolderDto(f.getId(), f.getName(), f.getParentId(),
                permissionService.effectiveLevel(user.getId(), f.getId())))
            .filter(dto -> dto.access() != AccessLevel.NONE)
            .toList();
    }

    /** UC3: podfolder - wymaga upoważnienia do zapisu w folderze nadrzędnym. */
    @Transactional
    public FolderDto createFolder(Long parentId, NewFolder req, AppUser user) {
        Folder parent = folders.findById(parentId)
            .orElseThrow(() -> new NotFoundException("Folder", parentId));
        AccessLevel level = permissionService.require(user.getId(), parentId, AccessLevel.WRITE);
        String name = req.name().trim();
        if (folders.existsByParentIdAndName(parentId, name)) {
            throw new ConflictException("Folder o nazwie \"" + name + "\" już istnieje w tym folderze");
        }
        Folder folder = folders.save(new Folder(name, parent.getRepository(), parentId));
        return new FolderDto(folder.getId(), folder.getName(), folder.getParentId(), level);
    }

    private void createDefaultDictionary(Repo repo) {
        java.util.Map<String, RoleDefinition> created = new java.util.LinkedHashMap<>();
        int order = 1;
        for (DefaultAction a : DEFAULT_DICTIONARY) {
            RoleDefinition role = created.computeIfAbsent(a.role(),
                name -> roles.save(new RoleDefinition(repo.getId(), name)));
            actions.save(new ActionType(role, a.action(), a.stage(), order++));
        }
    }
}
