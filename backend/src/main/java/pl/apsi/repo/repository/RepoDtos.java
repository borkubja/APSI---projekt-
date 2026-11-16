package pl.apsi.repo.repository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Obiekty przesyłane przez REST API repozytoriów i folderów. */
public final class RepoDtos {

    private RepoDtos() {
    }

    public record RepositoryDto(Long id, String name, String description, String owner) {
    }

    /** access = efektywny poziom upoważnienia zalogowanego użytkownika (READ albo WRITE). */
    public record FolderDto(Long id, String name, Long parentId, AccessLevel access) {
    }

    /** Upoważnienie nadane bezpośrednio na folderze. */
    public record PermissionDto(Long userId, String displayName, String username, AccessLevel level) {
    }

    public record GrantRequest(@NotNull Long userId, @NotNull AccessLevel level) {
    }

    public record NewRepository(@NotBlank @Size(max = 200) String name, @Size(max = 4000) String description) {
    }

    public record NewFolder(@NotBlank @Size(max = 200) String name) {
    }
}
