package pl.apsi.repo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** UC3 (repozytorium i folder) i UC7 (dokument): zapis przez API, odczyt z innego konta i sprawdzenie w bazie. */
class DataEntryIntegrationTest extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void createdRepositoryHasRootFolderOwnerPermissionAndDefaultWorkflowDictionary() {
        String name = "Repo-" + UUID.randomUUID();

        Response created = postJson("/api/repositories", "ewa.zielinska", Map.of("name", name, "description", "opis"));

        assertThat(created.status()).isEqualTo(201);
        long repoId = created.json().path("id").asLong();
        assertThat(created.json().path("owner").asText()).isEqualTo("Ewa Zielinska");
        // twórca widzi repozytorium (wcześniej nie widział żadnego), inni nie
        assertThat(get("/api/repositories", "ewa.zielinska").json().toString()).contains(name);
        assertThat(get("/api/repositories", "jan.kowalski").json().toString()).doesNotContain(name);
        // w bazie: folder główny, upoważnienie do zapisu i słownik z działaniem początkowym i zatwierdzającym
        assertThat(jdbc.queryForObject("select count(*) from folder where repository_id = ? and parent_id is null",
            Integer.class, repoId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
            select count(*) from folder_permission p join folder f on f.id = p.folder_id
            where f.repository_id = ? and p.level = 'WRITE'""", Integer.class, repoId)).isEqualTo(1);
        assertThat(jdbc.queryForList("""
            select distinct a.stage from action_type a join role_definition r on r.id = a.role_id
            where r.repository_id = ?""", String.class, repoId)).contains("START", "END");
    }

    @Test
    void subfolderCreatedByWriterIsVisibleToOtherAuthorisedUser() {
        String name = "Folder-" + UUID.randomUUID();

        Response created = postJson("/api/folders/3/folders", "anna.nowak", Map.of("name", name));

        assertThat(created.status()).isEqualTo(201);
        assertThat(created.json().path("parentId").asLong()).isEqualTo(3);
        assertThat(get("/api/repositories/1/folders", "piotr.wisniewski").json().toString()).contains(name);
    }

    @Test
    void readOnlyUserCannotCreateFolderOrDocument() {
        assertThat(postJson("/api/folders/2/folders", "piotr.wisniewski", Map.of("name", "X")).status())
            .isEqualTo(403);
        assertThat(postJson("/api/folders/2/documents", "piotr.wisniewski", Map.of("title", "X")).status())
            .isEqualTo(403);
    }

    @Test
    void documentAddedByOneUserIsVisibleToAnotherAndStoredWithFirstVersionAndStartEvent() {
        String title = "Dokument-" + UUID.randomUUID();

        Response created = postJson("/api/folders/2/documents", "jan.kowalski",
            Map.of("title", title, "description", "Opis", "initialVersion", "1.0"));

        assertThat(created.status()).isEqualTo(201);
        assertThat(created.json().path("latestVersion").asText()).isEqualTo("1.0");
        assertThat(created.json().path("lastAction").asText()).isEqualTo("Utworzyłem");
        // widoczny z innego konta (zapis i odczyt)
        for (String user : new String[] {"anna.nowak", "piotr.wisniewski"}) {
            JsonNode docs = get("/api/folders/2/documents", user).json();
            assertThat(docs.toString()).contains(title);
        }
        // w bazie: dokument, pierwsza wersja bez rodzica i zdarzenie początkowe obiegu
        Long docId = jdbc.queryForObject("select id from document where title = ?", Long.class, title);
        assertThat(jdbc.queryForObject(
            "select count(*) from document_version where document_id = ? and parent_version_id is null and version_label = '1.0'",
            Integer.class, docId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
            select count(*) from version_event e join document_version v on v.id = e.version_id
            where v.document_id = ?""", Integer.class, docId)).isEqualTo(1);
    }

    @Test
    void documentWithoutTitleOrWithBadVersionLabelIsRejectedAndNothingIsSaved() {
        int before = jdbc.queryForObject("select count(*) from document", Integer.class);

        assertThat(postJson("/api/folders/2/documents", "jan.kowalski", Map.of("title", "")).status()).isEqualTo(400);
        assertThat(postJson("/api/folders/2/documents", "jan.kowalski",
            Map.of("title", "T", "initialVersion", "v1")).status()).isEqualTo(400);

        assertThat(jdbc.queryForObject("select count(*) from document", Integer.class)).isEqualTo(before);
    }

    @Test
    void documentInUnknownFolderReturns404() {
        assertThat(postJson("/api/folders/999999/documents", "jan.kowalski", Map.of("title", "T")).status())
            .isEqualTo(404);
    }
}
