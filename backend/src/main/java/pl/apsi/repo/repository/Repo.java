package pl.apsi.repo.repository;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import pl.apsi.repo.user.AppUser;

/**
 * Repozytorium - folder najwyższego poziomu z własnym słownikiem ról i działań
 * (a w kolejnych iteracjach także pól metadanych). Nazwa klasy "Repo", żeby nie
 * myliła się z adnotacją i interfejsami Spring Data.
 */
@Entity
@Table(name = "repository")
public class Repo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Repo() {
    }

    public Repo(String name, String description, AppUser owner) {
        this.name = name;
        this.description = description;
        this.owner = owner;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public AppUser getOwner() { return owner; }
    public Instant getCreatedAt() { return createdAt; }
}
