package pl.apsi.repo.integration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Testy integracyjne przez pełny stos HTTP: prawdziwy serwer (losowy port), filtry Spring Security,
 * prawdziwy PostgreSQL w kontenerze (Testcontainers) z migracjami Flyway i danymi demo.
 *
 * <p>Jedyne, co podmieniamy, to dekoder JWT: zamiast pobierać klucze z Keycloak, akceptuje on token
 * będący samą nazwą użytkownika (np. {@code jan.kowalski}) i zwraca claimy takie, jakie wystawia Keycloak
 * dla kont z {@code keycloak/realm-apsi.json}. Dzięki temu testujemy całą resztę: filtr uwierzytelnienia,
 * autoryzację, kontrolery, serwisy, JPA i bazę.
 *
 * <p>Dlaczego testy {@code @WebMvcTest} nie wystarczają: MockMvc nie symuluje kontenera serwletów, więc
 * nigdy nie dochodzi do wewnętrznego przekierowania na {@code /error}. Błędy zależne od tego przekierowania
 * widać dopiero tutaj.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(IntegrationTestBase.Infrastructure.class)
public abstract class IntegrationTestBase {

    /** Konta z danych demo (V2) - klucz to nazwa użytkownika, wartość to claim "sub" (= keycloak_id). */
    private static final Map<String, String> SUBJECTS = Map.of(
        "jan.kowalski", "0f1c2a3b-0000-4000-8000-000000000001",
        "anna.nowak", "0f1c2a3b-0000-4000-8000-000000000002",
        "piotr.wisniewski", "0f1c2a3b-0000-4000-8000-000000000003",
        "ewa.zielinska", "0f1c2a3b-0000-4000-8000-000000000004");

    @TestConfiguration(proxyBeanMethods = false)
    static class Infrastructure {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>("postgres:16");
        }

        @Bean
        @Primary
        JwtDecoder testJwtDecoder() {
            return token -> {
                String subject = SUBJECTS.get(token);
                if (subject == null) {
                    throw new BadJwtException("Nieznany token testowy: " + token);
                }
                String[] name = token.split("\\.");
                return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject(subject)
                    .claim("preferred_username", token)
                    .claim("given_name", capitalize(name[0]))
                    .claim("family_name", capitalize(name[1]))
                    .claim("email", token + "@apsi.local")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(300))
                    .build();
            };
        }

        private static String capitalize(String s) {
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }

    /** Odpowiedź HTTP z wygodnym dostępem do JSON-a. */
    public record Response(int status, String body, String contentType) {
        public JsonNode json() {
            try {
                return MAPPER.readTree(body);
            } catch (IOException e) {
                throw new IllegalStateException("Odpowiedź nie jest JSON-em: [" + body + "]", e);
            }
        }
    }

    static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper mapper;

    protected Response get(String path, String user) {
        return send("GET", path, user, null, null);
    }

    protected Response postJson(String path, String user, Object body) {
        try {
            return send("POST", path, user, "application/json", mapper.writeValueAsString(body));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Surowe ciało - pozwala wysłać niepoprawny JSON. */
    protected Response postRaw(String path, String user, String contentType, String body) {
        return send("POST", path, user, contentType, body);
    }

    /** Formularz multipart: pola tekstowe (mogą się powtarzać) i opcjonalny plik w polu {@code file}. */
    public static final class Form {
        private static final String CRLF = "\r\n";
        private final String boundary = "----apsi" + System.nanoTime();
        private final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();

        public Form field(String name, String value) {
            part("Content-Disposition: form-data; name=\"" + name + "\"", value);
            return this;
        }

        public Form file(String fileName, String contentType, String content) {
            part("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + CRLF
                + "Content-Type: " + contentType, content);
            return this;
        }

        private void part(String headers, String content) {
            String text = "--" + boundary + CRLF + headers + CRLF + CRLF + content + CRLF;
            out.writeBytes(text.getBytes(StandardCharsets.UTF_8));
        }

        String contentType() {
            return "multipart/form-data; boundary=" + boundary;
        }

        byte[] bytes() {
            java.io.ByteArrayOutputStream all = new java.io.ByteArrayOutputStream();
            all.writeBytes(out.toByteArray());
            all.writeBytes(("--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8));
            return all.toByteArray();
        }
    }

    protected Response postForm(String path, String user, Form form) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Content-Type", form.contentType())
            .header("Authorization", "Bearer " + user)
            .POST(BodyPublishers.ofByteArray(form.bytes()));
        return execute(request);
    }

    protected Response putJson(String path, String user, Object body) {
        try {
            return send("PUT", path, user, "application/json", mapper.writeValueAsString(body));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected Response delete(String path, String user) {
        return send("DELETE", path, user, null, null);
    }

    /** user == null oznacza brak nagłówka Authorization. */
    protected Response send(String method, String path, String user, String contentType, String body) {
        BodyPublisher publisher = body == null ? BodyPublishers.noBody()
            : BodyPublishers.ofString(body, StandardCharsets.UTF_8);
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .method(method, publisher);
        if (contentType != null) {
            request.header("Content-Type", contentType);
        }
        if (user != null) {
            request.header("Authorization", "Bearer " + user);
        }
        return execute(request);
    }

    private Response execute(HttpRequest.Builder request) {
        try {
            HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body(),
                response.headers().firstValue("Content-Type").orElse(""));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
