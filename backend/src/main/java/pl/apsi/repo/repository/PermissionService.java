package pl.apsi.repo.repository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pl.apsi.repo.config.ForbiddenException;
import pl.apsi.repo.config.NotFoundException;

/**
 * Sprawdza upoważnienia do folderów. Upoważnienie nadane na folderze obowiązuje
 * także we wszystkich jego podfolderach; liczy się najwyższy poziom znaleziony
 * na drodze od folderu do folderu głównego repozytorium.
 */
@Service
public class PermissionService {

    private final FolderRepository folders;
    private final FolderPermissionRepository permissions;

    public PermissionService(FolderRepository folders, FolderPermissionRepository permissions) {
        this.folders = folders;
        this.permissions = permissions;
    }

    @Transactional(readOnly = true)
    public AccessLevel effectiveLevel(Long userId, Long folderId) {
        AccessLevel level = AccessLevel.NONE;
        Long current = folderId;
        int guard = 0;
        while (current != null && guard++ < 100) {
            Folder folder = folders.findById(current)
                .orElseThrow(() -> new NotFoundException("Folder", folderId));
            AccessLevel here = permissions.findByFolderIdAndUserId(current, userId)
                .map(FolderPermission::getLevel)
                .orElse(AccessLevel.NONE);
            level = AccessLevel.max(level, here);
            if (level == AccessLevel.WRITE) {
                break; // wyżej już nic nie zmieni
            }
            current = folder.getParentId();
        }
        return level;
    }

    /** Rzuca ForbiddenException (HTTP 403), gdy użytkownik nie ma wymaganego poziomu. */
    public AccessLevel require(Long userId, Long folderId, AccessLevel required) {
        AccessLevel level = effectiveLevel(userId, folderId);
        if (!level.allows(required)) {
            throw new ForbiddenException(required == AccessLevel.WRITE
                ? "Brak upoważnienia do zapisu w tym folderze"
                : "Brak upoważnienia do tego folderu");
        }
        return level;
    }
}
