package pl.apsi.repo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;

/**
 * Funkcje wyprzedzające plan (iteracje 2-3), które są już w kodzie: plik pierwszej wersji (UC7), nowa wersja (UC4),
 * pobranie pliku (UC15), zatwierdzenie (UC11) i widoczność dokumentu (UC16).
 */
class DocumentLifecycleIntegrationTest extends IntegrationTestBase {

    private long createDocument(String user, String title, String content, String... viewerIds) {
        Form form = new Form().field("title", title).file("notatka.txt", "text/plain", content);
        for (String id : viewerIds) {
            form.field("viewerIds", id);
        }
        Response r = postForm("/api/folders/2/documents", user, form);
        assertThat(r.status()).as(r.body()).isEqualTo(201);
        return r.json().path("id").asLong();
    }

    private JsonNode versions(long documentId, String user) {
        return get("/api/documents/" + documentId + "/versions", user).json();
    }

    @Test
    void fileCanBeDownloadedByAnyReader() {
        long id = createDocument("jan.kowalski", "Plik-" + UUID.randomUUID(), "treść pierwszej wersji");
        long versionId = versions(id, "jan.kowalski").get(0).path("id").asLong();

        Response file = get("/api/versions/" + versionId + "/file", "piotr.wisniewski");

        assertThat(file.status()).isEqualTo(200);
        assertThat(file.body()).isEqualTo("treść pierwszej wersji");
        assertThat(get("/api/versions/" + versionId + "/file", "ewa.zielinska").status()).isEqualTo(403);
    }

    @Test
    void versionWithoutFileReturns404() {
        assertThat(get("/api/versions/1/file", "jan.kowalski").status()).isEqualTo(404);
    }

    @Test
    void newVersionGetsNextLabelParentAndWorkflowEvent() {
        long id = createDocument("jan.kowalski", "Wersje-" + UUID.randomUUID(), "v1");

        Response created = postForm("/api/documents/" + id + "/versions", "anna.nowak",
            new Form().field("changeNote", "Poprawka").file("notatka.txt", "text/plain", "v2"));

        assertThat(created.status()).isEqualTo(201);
        assertThat(created.json().path("versionLabel").asText()).isEqualTo("0.2");
        assertThat(created.json().path("author").asText()).isEqualTo("Anna Nowak");
        assertThat(created.json().path("history").get(0).path("action").asText()).isEqualTo("Edytowałem");
        assertThat(versions(id, "piotr.wisniewski")).hasSize(2);
    }

    @Test
    void readOnlyUserCannotAddVersionOrApprove() {
        long id = createDocument("jan.kowalski", "Zapis-" + UUID.randomUUID(), "v1");
        long versionId = versions(id, "jan.kowalski").get(0).path("id").asLong();

        assertThat(postForm("/api/documents/" + id + "/versions", "piotr.wisniewski",
            new Form().file("a.txt", "text/plain", "x")).status()).isEqualTo(403);
        assertThat(postJson("/api/versions/" + versionId + "/approve", "piotr.wisniewski", Map.of()).status())
            .isEqualTo(403);
    }

    @Test
    void approvalLocksVersionAndCannotBeRepeated() {
        long id = createDocument("jan.kowalski", "Zatwierdzenie-" + UUID.randomUUID(), "v1");
        long versionId = versions(id, "jan.kowalski").get(0).path("id").asLong();

        Response approved = postJson("/api/versions/" + versionId + "/approve", "anna.nowak",
            Map.of("comment", "Zgodne z wymaganiami"));

        assertThat(approved.status()).isEqualTo(200);
        assertThat(approved.json().path("approved").asBoolean()).isTrue();
        JsonNode history = approved.json().path("history");
        assertThat(history.get(history.size() - 1).path("action").asText()).isEqualTo("Zatwierdziłem");
        assertThat(history.get(history.size() - 1).path("comment").asText()).isEqualTo("Zgodne z wymaganiami");
        assertThat(postJson("/api/versions/" + versionId + "/approve", "anna.nowak", Map.of()).status())
            .isEqualTo(409);
    }

    @Test
    void restrictedDocumentIsVisibleOnlyToAuthorWritersAndChosenViewers() {
        String title = "Poufny-" + UUID.randomUUID();
        // piotr (tylko odczyt) nie jest wskazany; anna ma zapis do folderu, więc widzi zawsze
        long id = createDocument("jan.kowalski", title, "tajne");

        assertThat(get("/api/folders/2/documents", "piotr.wisniewski").json().toString()).contains(title);

        Response hide = putJson("/api/documents/" + id + "/visibility", "jan.kowalski",
            Map.of("restricted", true, "viewerIds", java.util.List.of()));
        assertThat(hide.status()).isEqualTo(200);
        assertThat(hide.json().path("restricted").asBoolean()).isTrue();

        assertThat(get("/api/folders/2/documents", "piotr.wisniewski").json().toString()).doesNotContain(title);
        assertThat(get("/api/documents/" + id + "/versions", "piotr.wisniewski").status()).isEqualTo(403);
        assertThat(get("/api/folders/2/documents", "anna.nowak").json().toString()).contains(title);

        // autor wskazuje Piotra (id 3 z danych demo) - dokument znów jest dla niego widoczny
        putJson("/api/documents/" + id + "/visibility", "jan.kowalski",
            Map.of("restricted", true, "viewerIds", java.util.List.of(3)));
        assertThat(get("/api/folders/2/documents", "piotr.wisniewski").json().toString()).contains(title);
    }

    @Test
    void visibilityChosenWhileAddingTheDocument() {
        String title = "Dla-Anny-" + UUID.randomUUID();
        createDocument("jan.kowalski", title, "x", "2");

        assertThat(get("/api/folders/2/documents", "piotr.wisniewski").json().toString()).doesNotContain(title);
        assertThat(get("/api/folders/2/documents", "anna.nowak").json().toString()).contains(title);
    }

    @Test
    void unknownViewerIsRejected() {
        Response r = postForm("/api/folders/2/documents", "jan.kowalski",
            new Form().field("title", "T").field("viewerIds", "999999"));

        assertThat(r.status()).isEqualTo(400);
    }

    @Test
    void uploadWithoutTitleIsRejected() {
        Response r = postForm("/api/folders/2/documents", "jan.kowalski",
            new Form().field("title", " ").file("a.txt", "text/plain", "x"));

        assertThat(r.status()).isEqualTo(400);
    }
}
