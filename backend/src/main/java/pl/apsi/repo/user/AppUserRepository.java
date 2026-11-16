package pl.apsi.repo.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    java.util.List<AppUser> findAllByOrderByLastNameAscFirstNameAsc();

    Optional<AppUser> findByKeycloakId(String keycloakId);
}
