package pl.apsi.repo.repository;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Upoważnienie użytkownika do folderu (odczyt albo zapis); dziedziczone przez podfoldery. */
@Entity
@Table(name = "folder_permission")
public class FolderPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "folder_id", nullable = false)
    private Long folderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessLevel level;

    @Column(name = "granted_by")
    private Long grantedBy;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt = Instant.now();

    protected FolderPermission() {
    }

    public FolderPermission(Long folderId, Long userId, AccessLevel level, Long grantedBy) {
        if (level == AccessLevel.NONE) {
            throw new IllegalArgumentException("Upoważnienie musi być do odczytu albo zapisu");
        }
        this.folderId = folderId;
        this.userId = userId;
        this.level = level;
        this.grantedBy = grantedBy;
    }

    public Long getId() { return id; }
    public Long getFolderId() { return folderId; }
    public Long getUserId() { return userId; }
    public AccessLevel getLevel() { return level; }
    public Long getGrantedBy() { return grantedBy; }
    public Instant getGrantedAt() { return grantedAt; }
}
