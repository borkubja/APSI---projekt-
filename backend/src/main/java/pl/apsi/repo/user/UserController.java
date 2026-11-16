package pl.apsi.repo.user;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lista użytkowników systemu - do wyboru osób przy nadawaniu upoważnień i widoczności dokumentu. */
@RestController
@RequestMapping("/api/users")
public class UserController {

    public record UserDto(Long id, String username, String displayName) {
    }

    private final AppUserRepository users;

    public UserController(AppUserRepository users) {
        this.users = users;
    }

    @GetMapping
    public List<UserDto> list() {
        return users.findAllByOrderByLastNameAscFirstNameAsc().stream()
            .map(u -> new UserDto(u.getId(), u.getUsername(), u.getDisplayName()))
            .toList();
    }
}
