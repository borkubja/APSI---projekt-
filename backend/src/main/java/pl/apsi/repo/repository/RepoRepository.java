package pl.apsi.repo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepoRepository extends JpaRepository<Repo, Long> {

    /** Repozytoria, w których użytkownik ma jakiekolwiek upoważnienie. */
    @Query("""
        select distinct r from Repo r
        where exists (
            select 1 from FolderPermission p, Folder f
            where p.folderId = f.id and f.repository = r and p.userId = :userId)
        order by r.name""")
    List<Repo> findAccessibleBy(@Param("userId") Long userId);

    boolean existsByName(String name);
}
