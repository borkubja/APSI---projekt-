package pl.apsi.repo.repository;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pl.apsi.repo.config.NotFoundException;
import pl.apsi.repo.repository.RepoDtos.PermissionDto;
import pl.apsi.repo.user.AppUser;
import pl.apsi.repo.user.AppUserRepository;

/** UC6: nadawanie i odbieranie upoważnień do folderu. Wymaga zapisu do folderu; właściciela repozytorium nie można odebrać. */
@Service
public class FolderAccessService {

    private final FolderRepository folders;
    private final FolderPermissionRepository permissions;
    private final AppUserRepository users;
    private final PermissionService permissionService;

    public FolderAccessService(FolderRepository folders, FolderPermissionRepository permissions,
                               AppUserRepository users, PermissionService permissionService) {
        this.folders = folders;
        this.permissions = permissions;
        this.users = users;
        this.permissionService = permissionService;
    }

    /** Upoważnienia nadane bezpośrednio na tym folderze (dziedziczone z wyższych folderów widać w efektywnym poziomie). */
    @Transactional(readOnly = true)
    public List<PermissionDto> list(Long folderId, AppUser actor) {
        folder(folderId);
        permissionService.require(actor.getId(), folderId, AccessLevel.WRITE);
        return permissions.findByFolderId(folderId).stream()
            .map(p -> {
                AppUser u = users.findById(p.getUserId()).orElseThrow();
                return new PermissionDto(u.getId(), u.getDisplayName(), u.getUsername(), p.getLevel());
            })
            .sorted(java.util.Comparator.comparing(PermissionDto::displayName))
            .toList();
    }

    @Transactional
    public PermissionDto grant(Long folderId, Long userId, AccessLevel level, AppUser actor) {
        Folder folder = folder(folderId);
        permissionService.require(actor.getId(), folderId, AccessLevel.WRITE);
        if (level == null || level == AccessLevel.NONE) {
            throw new IllegalArgumentException("Poziom upoważnienia: READ albo WRITE");
        }
        AppUser target = users.findById(userId)
            .orElseThrow(() -> new NotFoundException("Użytkownik", userId));
        if (isOwner(folder, userId)) {
            throw new IllegalArgumentException("Nie zmieniamy upoważnień właściciela repozytorium");
        }
        FolderPermission existing = permissions.findByFolderIdAndUserId(folderId, userId).orElse(null);
        if (existing != null) {
            permissions.delete(existing);
            permissions.flush();
        }
        permissions.save(new FolderPermission(folderId, userId, level, actor.getId()));
        return new PermissionDto(target.getId(), target.getDisplayName(), target.getUsername(), level);
    }

    @Transactional
    public void revoke(Long folderId, Long userId, AppUser actor) {
        Folder folder = folder(folderId);
        permissionService.require(actor.getId(), folderId, AccessLevel.WRITE);
        if (isOwner(folder, userId)) {
            throw new IllegalArgumentException("Nie można odebrać upoważnień właściciela repozytorium");
        }
        permissions.findByFolderIdAndUserId(folderId, userId).ifPresent(permissions::delete);
    }

    private boolean isOwner(Folder folder, Long userId) {
        return folder.getRepository().getOwner().getId().equals(userId);
    }

    private Folder folder(Long folderId) {
        return folders.findById(folderId).orElseThrow(() -> new NotFoundException("Folder", folderId));
    }
}
