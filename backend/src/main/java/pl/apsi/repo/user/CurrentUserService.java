package pl.apsi.repo.user;

import java.util.Objects;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Konta zakłada administrator w Keycloak. Przy pierwszym zapytaniu z tokenem
 * zapisujemy użytkownika w bazie (lub odświeżamy jego dane), żeby móc go
 * wskazywać jako autora dokumentów i wersji. Zapis następuje tylko wtedy,
 * gdy dane z tokenu różnią się od zapisanych - zwykłe odczyty nie piszą do bazy.
 */
@Service
public class CurrentUserService {

    private final AppUserRepository users;

    public CurrentUserService(AppUserRepository users) {
        this.users = users;
    }

    @Transactional
    public AppUser fromToken(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        AppUser user = users.findByKeycloakId(jwt.getSubject()).orElse(null);
        if (user == null) {
            user = new AppUser(jwt.getSubject(), username);
        } else if (sameData(user, jwt)) {
            return user;
        }
        user.setUsername(username);
        user.setFirstName(jwt.getClaimAsString("given_name"));
        user.setLastName(jwt.getClaimAsString("family_name"));
        user.setEmail(jwt.getClaimAsString("email"));
        return users.save(user);
    }

    private static boolean sameData(AppUser user, Jwt jwt) {
        return Objects.equals(user.getUsername(), jwt.getClaimAsString("preferred_username"))
            && Objects.equals(user.getFirstName(), jwt.getClaimAsString("given_name"))
            && Objects.equals(user.getLastName(), jwt.getClaimAsString("family_name"))
            && Objects.equals(user.getEmail(), jwt.getClaimAsString("email"));
    }
}
