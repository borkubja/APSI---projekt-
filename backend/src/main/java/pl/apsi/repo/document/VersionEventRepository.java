package pl.apsi.repo.document;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VersionEventRepository extends JpaRepository<VersionEvent, Long> {

    List<VersionEvent> findByVersionIdOrderByIdAsc(Long versionId);

    Optional<VersionEvent> findFirstByVersionIdOrderByIdDesc(Long versionId);
}
