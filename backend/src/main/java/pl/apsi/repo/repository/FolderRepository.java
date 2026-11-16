package pl.apsi.repo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    boolean existsByParentIdAndName(Long parentId, String name);

    List<Folder> findByRepositoryIdOrderByNameAsc(Long repositoryId);
}
