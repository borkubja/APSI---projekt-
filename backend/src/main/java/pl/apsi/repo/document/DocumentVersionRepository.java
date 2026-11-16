package pl.apsi.repo.document;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    List<DocumentVersion> findByDocumentIdOrderByIdAsc(Long documentId);

    /** Ostatnio utworzona wersja dokumentu (na dowolnej gałęzi). */
    Optional<DocumentVersion> findFirstByDocumentIdOrderByIdDesc(Long documentId);

    boolean existsByDocumentIdAndVersionLabel(Long documentId, String versionLabel);
}
