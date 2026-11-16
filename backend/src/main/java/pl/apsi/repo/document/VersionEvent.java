package pl.apsi.repo.document;

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

import pl.apsi.repo.repository.ActionType;
import pl.apsi.repo.user.AppUser;

/** Wpis w historii obiegu wersji: kto, jakie działanie z listy, kiedy, z opcjonalnym komentarzem. */
@Entity
@Table(name = "version_event")
public class VersionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id")
    private DocumentVersion version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "action_type_id")
    private ActionType actionType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected VersionEvent() {
    }

    public VersionEvent(DocumentVersion version, ActionType actionType, AppUser user, String comment) {
        this.version = version;
        this.actionType = actionType;
        this.user = user;
        this.comment = comment;
    }

    public Long getId() { return id; }
    public DocumentVersion getVersion() { return version; }
    public ActionType getActionType() { return actionType; }
    public AppUser getUser() { return user; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }
}
