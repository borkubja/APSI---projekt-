package pl.apsi.repo.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Element struktury repozytorium; parentId == null oznacza folder główny repozytorium. */
@Entity
@Table(name = "folder")
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id")
    private Repo repository;

    @Column(name = "parent_id")
    private Long parentId;

    protected Folder() {
    }

    public Folder(String name, Repo repository, Long parentId) {
        this.name = name;
        this.repository = repository;
        this.parentId = parentId;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Repo getRepository() { return repository; }
    public Long getParentId() { return parentId; }
}
