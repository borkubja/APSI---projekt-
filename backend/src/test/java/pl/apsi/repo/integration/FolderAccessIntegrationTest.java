package pl.apsi.repo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** UC6: nadawanie i odbieranie upoważnień do folderu (dziedziczenie przez podfoldery). */
class FolderAccessIntegrationTest extends IntegrationTestBase {

    private static final long EWA = 4;

    @Test
    void grantedPermissionIsInheritedBySubfoldersAndRevocationRemovesIt() {
        String sub = "Podfolder-" + UUID.randomUUID();
        long projectFolder = 3;
        long subId = postJson("/api/folders/" + projectFolder + "/folders", "jan.kowalski", Map.of("name", sub))
            .json().path("id").asLong();
        assertThat(get("/api/folders/" + subId + "/documents", "ewa.zielinska").status()).isEqualTo(403);

        Response grant = putJson("/api/folders/" + projectFolder + "/permissions", "jan.kowalski",
            Map.of("userId", EWA, "level", "READ"));
        assertThat(grant.status()).isEqualTo(200);
        try {
            // upoważnienie na folderze "Projekt" obejmuje podfolder, ale nie sąsiedni folder "Wymagania"
            assertThat(get("/api/folders/" + subId + "/documents", "ewa.zielinska").status()).isEqualTo(200);
            assertThat(get("/api/folders/2/documents", "ewa.zielinska").status()).isEqualTo(403);
            assertThat(get("/api/folders/" + projectFolder + "/permissions", "jan.kowalski").json().toString())
                .contains("Ewa Zielinska");
            // odczyt nie pozwala na zapis
            assertThat(postJson("/api/folders/" + subId + "/folders", "ewa.zielinska", Map.of("name", "X")).status())
                .isEqualTo(403);
        } finally {
            assertThat(delete("/api/folders/" + projectFolder + "/permissions/" + EWA, "jan.kowalski").status())
                .isEqualTo(204);
        }
        assertThat(get("/api/folders/" + subId + "/documents", "ewa.zielinska").status()).isEqualTo(403);
    }

    @Test
    void onlyWritersManagePermissions() {
        assertThat(get("/api/folders/2/permissions", "piotr.wisniewski").status()).isEqualTo(403);
        assertThat(putJson("/api/folders/2/permissions", "piotr.wisniewski",
            Map.of("userId", EWA, "level", "READ")).status()).isEqualTo(403);
    }

    @Test
    void ownerPermissionCannotBeChangedAndLevelIsRequired() {
        assertThat(putJson("/api/folders/1/permissions", "anna.nowak",
            Map.of("userId", 1, "level", "READ")).status()).isEqualTo(400);
        assertThat(delete("/api/folders/1/permissions/1", "anna.nowak").status()).isEqualTo(400);
        assertThat(putJson("/api/folders/1/permissions", "jan.kowalski", Map.of("userId", EWA)).status())
            .isEqualTo(400);
    }

    @Test
    void userDirectoryIsAvailableToLoggedInUsers() {
        Response users = get("/api/users", "jan.kowalski");

        assertThat(users.status()).isEqualTo(200);
        assertThat(users.json().size()).isGreaterThanOrEqualTo(4);
        assertThat(get("/api/users", null).status()).isEqualTo(401);
    }
}
