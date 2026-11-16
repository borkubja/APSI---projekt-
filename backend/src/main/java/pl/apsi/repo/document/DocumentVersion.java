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

import pl.apsi.repo.user.AppUser;

/**
 * Wersja dokumentu - węzeł drzewa wersji. parentVersionId == null oznacza korzeń;
 * kilka wersji o wspólnym rodzicu to wersje równoległe. Po zatwierdzeniu
 * (approved = true) wersja jest zablokowana. Plik (fileKey, sha256) dochodzi w iteracji 2.
 */
@Entity
@Table(name = "document_version")
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(name = "parent_version_id")
    private Long parentVersionId;

    @Column(name = "version_label", nullable = false)
    private String versionLabel;

    @Column(name = "change_note", columnDefinition = "text")
    private String changeNote;

    @Column(nullable = false)
    private boolean approved;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_key")
    private String fileKey;

    private String sha256;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "mime_type")
    private String mimeType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private AppUser author;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected DocumentVersion() {
    }

    public DocumentVersion(Document document, Long parentVersionId, String versionLabel, String changeNote, AppUser author) {
        this.document = document;
        this.parentVersionId = parentVersionId;
        this.versionLabel = versionLabel;
        this.changeNote = changeNote;
        this.author = author;
    }

    public void attachFile(String fileKey, String fileName, String sha256, long sizeBytes, String mimeType) {
        this.fileKey = fileKey;
        this.fileName = fileName;
        this.sha256 = sha256;
        this.sizeBytes = sizeBytes;
        this.mimeType = mimeType;
    }

    /** Zatwierdzenie blokuje wersję - nie można jej już zatwierdzić ponownie. */
    public void approve() {
        this.approved = true;
    }

    public String getFileName() { return fileName; }
    public Long getId() { return id; }
    public Document getDocument() { return document; }
    public Long getParentVersionId() { return parentVersionId; }
    public String getVersionLabel() { return versionLabel; }
    public String getChangeNote() { return changeNote; }
    public boolean isApproved() { return approved; }
    public String getFileKey() { return fileKey; }
    public String getSha256() { return sha256; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getMimeType() { return mimeType; }
    public AppUser getAuthor() { return author; }
    public Instant getCreatedAt() { return createdAt; }
}
