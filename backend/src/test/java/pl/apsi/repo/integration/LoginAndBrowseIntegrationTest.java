package pl.apsi.repo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** UC1 (logowanie) i UC2 (przeglądanie) na danych demo z migracji V2. */
class LoginAndBrowseIntegrationTest extends IntegrationTestBase {

    @ParameterizedTest
    @CsvSource({
        "jan.kowalski, Jan Kowalski",
        "anna.nowak, Anna Nowak",
        "piotr.wisniewski, Piotr Wisniewski",
        "ewa.zielinska, Ewa Zielinska"})
    void everyTestAccountCanLogInAndSeesItsProfile(String username, String displayName) {
        Response r = get("/api/me", username);

        assertThat(r.status()).isEqualTo(200);
        assertThat(r.json().path("username").asText()).isEqualTo(username);
        assertThat(r.json().path("displayName").asText()).isEqualTo(displayName);
        assertThat(r.json().path("email").asText()).isEqualTo(username + "@apsi.local");
    }

    @Test
    void invalidTokenIsRejected() {
        assertThat(get("/api/me", "nieznany.uzytkownik").status()).isEqualTo(401);
    }

    @Test
    void userWithAuthorisationSeesTheDemoRepositoryAndItsFolderTree() {
        Response repos = get("/api/repositories", "anna.nowak");
        assertThat(repos.status()).isEqualTo(200);
        assertThat(names(repos.json())).contains("Bank – system kredytowy");

        Response folders = get("/api/repositories/1/folders", "anna.nowak");
        // testy współdzielą bazę, więc mogą dojść inne foldery - sprawdzamy folder główny i trzy foldery demo
        assertThat(names(folders.json())).contains("Bank – system kredytowy", "Wymagania", "Projekt", "Testy");
        // upoważnienie nadane na folderze głównym jest dziedziczone przez podfoldery
        for (JsonNode folder : folders.json()) {
            assertThat(folder.path("access").asText()).isEqualTo("WRITE");
        }
    }

    @Test
    void readOnlyUserSeesTheSameFoldersWithReadAccess() {
        Response folders = get("/api/repositories/1/folders", "piotr.wisniewski");

        assertThat(folders.status()).isEqualTo(200);
        assertThat(names(folders.json())).contains("Bank – system kredytowy", "Wymagania", "Projekt", "Testy");
        for (JsonNode folder : folders.json()) {
            assertThat(folder.path("access").asText()).isEqualTo("READ");
        }
    }

    @Test
    void userWithoutAuthorisationSeesNothing() {
        // ewa nie ma upoważnienia do repozytorium demo (może mieć własne, założone w innych testach)
        assertThat(names(get("/api/repositories", "ewa.zielinska").json())).doesNotContain("Bank – system kredytowy");
        assertThat(get("/api/repositories/1/folders", "ewa.zielinska").json()).isEmpty();
        assertThat(get("/api/folders/2/documents", "ewa.zielinska").status()).isEqualTo(403);
    }

    @Test
    void documentListShowsLatestVersionAndLastWorkflowAction() {
        Response docs = get("/api/folders/2/documents", "piotr.wisniewski");

        assertThat(docs.status()).isEqualTo(200);
        JsonNode spec = find(docs.json(), "title", "Specyfikacja wymagań");
        assertThat(spec.path("latestVersion").asText()).isEqualTo("1.0.1");
        assertThat(spec.path("approved").asBoolean()).isFalse();
        assertThat(spec.path("lastAction").asText()).isEqualTo("Utworzyłem");
        assertThat(spec.path("author").asText()).isEqualTo("Jan Kowalski");
    }

    @Test
    void versionTreeExposesParentsForParallelVersions() {
        Response versions = get("/api/documents/1/versions", "piotr.wisniewski");

        assertThat(versions.status()).isEqualTo(200);
        JsonNode root = find(versions.json(), "versionLabel", "1.0");
        JsonNode next = find(versions.json(), "versionLabel", "1.1");
        JsonNode parallel = find(versions.json(), "versionLabel", "1.0.1");
        assertThat(root.path("parentVersionId").isNull()).isTrue();
        assertThat(root.path("approved").asBoolean()).isTrue();
        // wersje 1.1 i 1.0.1 mają wspólnego rodzica, czyli są równoległe
        assertThat(next.path("parentVersionId").asLong()).isEqualTo(root.path("id").asLong());
        assertThat(parallel.path("parentVersionId").asLong()).isEqualTo(root.path("id").asLong());
        assertThat(root.path("history")).isNotEmpty();
    }

    private static List<String> names(JsonNode array) {
        List<String> names = new ArrayList<>();
        array.forEach(n -> names.add(n.path("name").asText()));
        return names;
    }

    private static JsonNode find(JsonNode array, String field, String value) {
        for (JsonNode n : array) {
            if (value.equals(n.path(field).asText())) {
                return n;
            }
        }
        throw new AssertionError("Brak elementu " + field + "=" + value + " w " + array);
    }
}
