package pl.apsi.repo.document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import pl.apsi.repo.repository.Folder;
import pl.apsi.repo.user.AppUser;

/** Dokument logiczny; jego treść żyje w wersjach (DocumentVersion), które tworzą drzewo. */
@Entity
@Table(name = "document")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private AppUser author;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private boolean deleted;

    /** TRUE = widoczny tylko dla autora, osób z zapisem do folderu i osób z viewerIds. */
    @Column(nullable = false)
    private boolean restricted;

    @ElementCollection
    @CollectionTable(name = "document_viewer", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "user_id")
    private Set<Long> viewerIds = new HashSet<>();

    protected Document() {
    }

    public Document(String title, String description, Folder folder, AppUser author) {
        this.title = title;
        this.description = description;
        this.folder = folder;
        this.author = author;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Folder getFolder() { return folder; }
    public AppUser getAuthor() { return author; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isDeleted() { return deleted; }
    public boolean isRestricted() { return restricted; }
    public Set<Long> getViewerIds() { return viewerIds; }

    public void setVisibility(boolean restricted, Set<Long> viewers) {
        this.restricted = restricted;
        this.viewerIds.clear();
        if (restricted) {
            this.viewerIds.addAll(viewers);
        }
    }
}
