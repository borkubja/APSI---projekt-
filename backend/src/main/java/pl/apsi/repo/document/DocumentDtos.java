package pl.apsi.repo.document;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Obiekty przesyłane przez REST API dokumentów. Kontrolery nie zwracają encji. */
public final class DocumentDtos {

    private DocumentDtos() {
    }

    /**
     * lastAction = nazwa ostatniego działania na najnowszej wersji, np. "Zrecenzowałem".
     * restricted/viewerIds = ograniczona widoczność dokumentu (kto poza autorem i osobami z zapisem go widzi).
     */
    public record DocumentDto(Long id, String title, String description, String author, Instant createdAt,
                              String latestVersion, boolean approved, String lastAction,
                              Long latestVersionId, String fileName, boolean restricted, List<Long> viewerIds) {
    }

    public record EventDto(String action, String stage, String user, String comment, Instant createdAt) {
    }

    /** history = historia obiegu tej wersji (kto, co, kiedy). */
    public record VersionDto(Long id, String versionLabel, Long parentVersionId, boolean approved,
                             String changeNote, String author, Instant createdAt,
                             String fileName, Long sizeBytes, String mimeType, List<EventDto> history) {
    }

    /** Dodanie dokumentu; viewerIds niepuste = dokument widoczny tylko dla wskazanych osób. */
    public record NewDocument(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 4000) String description,
        @Pattern(regexp = "^\\d+(\\.\\d+)*$", message = "etykieta wersji w formacie np. 1.0 lub 1.2.1")
        String initialVersion,
        List<Long> viewerIds) {
    }

    public record VisibilityRequest(@NotNull Boolean restricted, List<Long> viewerIds) {
    }

    public record ApproveRequest(@Size(max = 4000) String comment) {
    }
}
