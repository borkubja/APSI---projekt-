package pl.apsi.repo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FolderPermissionRepository extends JpaRepository<FolderPermission, Long> {

    List<FolderPermission> findByFolderId(Long folderId);

    Optional<FolderPermission> findByFolderIdAndUserId(Long folderId, Long userId);
}
