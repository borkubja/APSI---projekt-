package pl.apsi.repo.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Rola w słowniku repozytorium, np. Recenzent. */
@Entity
@Table(name = "role_definition")
public class RoleDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repository_id", nullable = false)
    private Long repositoryId;

    @Column(nullable = false)
    private String name;

    protected RoleDefinition() {
    }

    public RoleDefinition(Long repositoryId, String name) {
        this.repositoryId = repositoryId;
        this.name = name;
    }

    public Long getId() { return id; }
    public Long getRepositoryId() { return repositoryId; }
    public String getName() { return name; }
}
