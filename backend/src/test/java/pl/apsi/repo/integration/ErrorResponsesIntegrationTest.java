package pl.apsi.repo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Kody i treść odpowiedzi błędów przez pełny stos HTTP.
 *
 * <p>Test odtwarza błąd z audytu: każdy błąd walidacji, niepoprawny JSON i duplikat zwracały 403 z pustym
 * ciałem. Przyczyna: {@code anyRequest().denyAll()} obejmowało też wewnętrzne przekierowanie kontenera
 * serwletów na {@code /error} (dispatcher ERROR), które Spring Boot wykonuje po {@code sendError(400)}.
 * Wcześniejszy {@code DocumentControllerTest} (@WebMvcTest + MockMvc) tego nie wykrywał, bo MockMvc nie ma
 * kontenera serwletów i nigdy nie robi przekierowania na /error - sprawdzał więc tylko kod ustawiony
 * w kontrolerze, a nie to, co faktycznie dostaje klient.
 */
class ErrorResponsesIntegrationTest extends IntegrationTestBase {

    private static final String JSON = "application/json";

    @Test
    void validationErrorReturns400WithFieldDetails() {
        Response r = postJson("/api/repositories", "jan.kowalski", Map.of("name", " "));

        assertThat(r.status()).isEqualTo(400);
        assertThat(r.contentType()).contains("application/problem+json");
        assertThat(r.json().path("status").asInt()).isEqualTo(400);
        assertThat(r.json().path("errors").path("name").asText()).isNotBlank();
    }

    @Test
    void malformedJsonReturns400() {
        Response r = postRaw("/api/repositories", "jan.kowalski", JSON, "{ to nie jest json");

        assertThat(r.status()).isEqualTo(400);
        assertThat(r.json().path("detail").asText()).contains("JSON");
    }

    @Test
    void duplicateFolderNameReturns409() {
        String name = "Duplikat-" + UUID.randomUUID();
        assertThat(postJson("/api/folders/1/folders", "jan.kowalski", Map.of("name", name)).status())
            .isEqualTo(201);

        Response second = postJson("/api/folders/1/folders", "jan.kowalski", Map.of("name", name));

        assertThat(second.status()).isEqualTo(409);
        assertThat(second.json().path("detail").asText()).contains(name);
    }

    @Test
    void duplicateRepositoryNameReturns409() {
        Response r = postJson("/api/repositories", "jan.kowalski", Map.of("name", "Bank – system kredytowy"));

        assertThat(r.status()).isEqualTo(409);
    }

    @Test
    void missingTokenReturns401() {
        assertThat(get("/api/me", null).status()).isEqualTo(401);
        assertThat(postJson("/api/repositories", null, Map.of("name", "X")).status()).isEqualTo(401);
    }

    @Test
    void missingPermissionReturns403WithBody() {
        Response r = get("/api/folders/2/documents", "ewa.zielinska");

        assertThat(r.status()).isEqualTo(403);
        assertThat(r.json().path("detail").asText()).isNotBlank();
    }

    @Test
    void unknownResourceReturns404WithBody() {
        Response r = get("/api/folders/999999/documents", "jan.kowalski");

        assertThat(r.status()).isEqualTo(404);
        assertThat(r.json().path("status").asInt()).isEqualTo(404);
    }
}
