package pl.apsi.repo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pl.apsi.repo.config.ForbiddenException;

/**
 * Dziedziczenie upoważnień: repozytorium (folder 1) -> Wymagania (2) -> Archiwum (3).
 * anna (10) ma zapis na folderze głównym, piotr (20) odczyt na folderze głównym
 * i zapis na Archiwum, ewa (30) nie ma żadnych upoważnień.
 */
class PermissionServiceTest {

    private final FolderRepository folders = mock(FolderRepository.class);
    private final FolderPermissionRepository permissions = mock(FolderPermissionRepository.class);
    private final PermissionService service = new PermissionService(folders, permissions);

    @BeforeEach
    void setUp() {
        Repo repo = mock(Repo.class);
        when(folders.findById(1L)).thenReturn(Optional.of(new Folder("Bank", repo, null)));
        when(folders.findById(2L)).thenReturn(Optional.of(new Folder("Wymagania", repo, 1L)));
        when(folders.findById(3L)).thenReturn(Optional.of(new Folder("Archiwum", repo, 2L)));

        when(permissions.findByFolderIdAndUserId(1L, 10L))
            .thenReturn(Optional.of(new FolderPermission(1L, 10L, AccessLevel.WRITE, 10L)));
        when(permissions.findByFolderIdAndUserId(1L, 20L))
            .thenReturn(Optional.of(new FolderPermission(1L, 20L, AccessLevel.READ, 10L)));
        when(permissions.findByFolderIdAndUserId(3L, 20L))
            .thenReturn(Optional.of(new FolderPermission(3L, 20L, AccessLevel.WRITE, 10L)));
        // pozostałe kombinacje: Mockito zwraca Optional.empty()
    }

    @Test
    void permissionOnRootIsInheritedBySubfolders() {
        assertThat(service.effectiveLevel(10L, 3L)).isEqualTo(AccessLevel.WRITE);
        assertThat(service.effectiveLevel(20L, 2L)).isEqualTo(AccessLevel.READ);
    }

    @Test
    void higherPermissionOnSubfolderWins() {
        assertThat(service.effectiveLevel(20L, 3L)).isEqualTo(AccessLevel.WRITE);
    }

    @Test
    void userWithoutPermissionHasNoAccess() {
        assertThat(service.effectiveLevel(30L, 2L)).isEqualTo(AccessLevel.NONE);
        assertThatThrownBy(() -> service.require(30L, 2L, AccessLevel.READ))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void readOnlyUserCannotWrite() {
        assertThatThrownBy(() -> service.require(20L, 2L, AccessLevel.WRITE))
            .isInstanceOf(ForbiddenException.class);
        assertThat(service.require(20L, 2L, AccessLevel.READ)).isEqualTo(AccessLevel.READ);
    }
}
