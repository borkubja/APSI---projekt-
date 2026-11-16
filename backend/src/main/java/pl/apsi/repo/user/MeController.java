package pl.apsi.repo.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    public record MeDto(Long id, String username, String displayName, String email) {
    }

    private final CurrentUserService currentUser;

    public MeController(CurrentUserService currentUser) {
        this.currentUser = currentUser;
    }

    /** Dane zalogowanego użytkownika; przy okazji zapisuje go w bazie lub odświeża jego dane. */
    @GetMapping("/api/me")
    public MeDto me(@AuthenticationPrincipal Jwt jwt) {
        AppUser user = currentUser.fromToken(jwt);
        return new MeDto(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail());
    }
}
