package pl.apsi.repo.repository;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pl.apsi.repo.repository.RepoDtos.FolderDto;
import pl.apsi.repo.repository.RepoDtos.GrantRequest;
import pl.apsi.repo.repository.RepoDtos.PermissionDto;
import pl.apsi.repo.repository.RepoDtos.NewFolder;
import pl.apsi.repo.repository.RepoDtos.NewRepository;
import pl.apsi.repo.repository.RepoDtos.RepositoryDto;
import pl.apsi.repo.user.CurrentUserService;

@RestController
@RequestMapping("/api")
public class RepoController {

    private final RepoService service;
    private final CurrentUserService currentUser;
    private final FolderAccessService access;

    public RepoController(RepoService service, CurrentUserService currentUser, FolderAccessService access) {
        this.access = access;
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/repositories")
    public List<RepositoryDto> list(@AuthenticationPrincipal Jwt jwt) {
        return service.accessibleRepositories(currentUser.fromToken(jwt));
    }

    @PostMapping("/repositories")
    @ResponseStatus(HttpStatus.CREATED)
    public RepositoryDto create(@Valid @RequestBody NewRepository req, @AuthenticationPrincipal Jwt jwt) {
        return service.createRepository(req, currentUser.fromToken(jwt));
    }

    @GetMapping("/repositories/{repositoryId}/folders")
    public List<FolderDto> folders(@PathVariable Long repositoryId, @AuthenticationPrincipal Jwt jwt) {
        return service.folders(repositoryId, currentUser.fromToken(jwt));
    }

    @PostMapping("/folders/{folderId}/folders")
    @ResponseStatus(HttpStatus.CREATED)
    public FolderDto createFolder(@PathVariable Long folderId, @Valid @RequestBody NewFolder req,
                                  @AuthenticationPrincipal Jwt jwt) {
        return service.createFolder(folderId, req, currentUser.fromToken(jwt));
    }

    @GetMapping("/folders/{folderId}/permissions")
    public List<PermissionDto> permissions(@PathVariable Long folderId, @AuthenticationPrincipal Jwt jwt) {
        return access.list(folderId, currentUser.fromToken(jwt));
    }

    @PutMapping("/folders/{folderId}/permissions")
    public PermissionDto grant(@PathVariable Long folderId, @Valid @RequestBody GrantRequest req,
                               @AuthenticationPrincipal Jwt jwt) {
        return access.grant(folderId, req.userId(), req.level(), currentUser.fromToken(jwt));
    }

    @DeleteMapping("/folders/{folderId}/permissions/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable Long folderId, @PathVariable Long userId, @AuthenticationPrincipal Jwt jwt) {
        access.revoke(folderId, userId, currentUser.fromToken(jwt));
    }
}
