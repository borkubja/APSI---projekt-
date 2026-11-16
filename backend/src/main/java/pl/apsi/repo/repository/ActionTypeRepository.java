package pl.apsi.repo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionTypeRepository extends JpaRepository<ActionType, Long> {

    /** Pierwsze (wg kolejności na liście) działanie danego etapu obiegu w słowniku repozytorium. */
    Optional<ActionType> findFirstByRoleRepositoryIdAndStageOrderBySortOrderAsc(Long repositoryId, ActionStage stage);
}
