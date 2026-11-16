package pl.apsi.repo.document;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pl.apsi.repo.config.ForbiddenException;
import pl.apsi.repo.config.SecurityConfig;
import pl.apsi.repo.document.DocumentDtos.DocumentDto;
import pl.apsi.repo.user.CurrentUserService;

/**
 * Testy warstwy HTTP: uwierzytelnienie, mapowanie odpowiedzi i kodów błędów.
 * Sama logika upoważnień jest testowana w PermissionServiceTest.
 */
@WebMvcTest(DocumentController.class)
@Import(SecurityConfig.class)
class DocumentControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    DocumentService service;

    @MockitoBean
    CurrentUserService currentUser;

    @MockitoBean
    JwtDecoder jwtDecoder; // nie łączymy się z Keycloak w testach

    private static final DocumentDto SPEC = new DocumentDto(1L, "Specyfikacja wymagań", null, "Jan Kowalski",
        Instant.parse("2026-11-01T10:00:00Z"), "1.1", false, "Edytowałem", 2L, null, false, List.of());

    @Test
    void anonymousUserIsRejected() throws Exception {
        mvc.perform(get("/api/folders/2/documents")).andExpect(status().isUnauthorized());
    }

    @Test
    void userWithReadAccessSeesDocuments() throws Exception {
        when(service.listInFolder(eq(2L), any())).thenReturn(List.of(SPEC));

        mvc.perform(get("/api/folders/2/documents").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Specyfikacja wymagań"))
            .andExpect(jsonPath("$[0].latestVersion").value("1.1"))
            .andExpect(jsonPath("$[0].lastAction").value("Edytowałem"));
    }

    @Test
    void userWithoutPermissionGets403() throws Exception {
        when(service.listInFolder(eq(2L), any())).thenThrow(new ForbiddenException("Brak upoważnienia do tego folderu"));

        mvc.perform(get("/api/folders/2/documents").with(jwt()))
            .andExpect(status().isForbidden());
    }

    @Test
    void readOnlyUserCannotCreateDocument() throws Exception {
        when(service.create(eq(2L), any(), any()))
            .thenThrow(new ForbiddenException("Brak upoważnienia do zapisu w tym folderze"));

        mvc.perform(post("/api/folders/2/documents").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"X\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void userWithWriteAccessCreatesDocument() throws Exception {
        when(service.create(eq(2L), any(), any())).thenReturn(SPEC);

        mvc.perform(post("/api/folders/2/documents").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Specyfikacja wymagań\",\"initialVersion\":\"1.0\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void invalidVersionLabelIsRejected() throws Exception {
        mvc.perform(post("/api/folders/2/documents").with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"X\",\"initialVersion\":\"v1\"}"))
            .andExpect(status().isBadRequest());
    }
}
