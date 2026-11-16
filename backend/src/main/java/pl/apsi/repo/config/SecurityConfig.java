package pl.apsi.repo.config;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Backend jest serwerem zasobów OAuth2: przyjmuje tokeny JWT wydane przez Keycloak.
 * Hierarchia użytkowników jest płaska - nie ma ról globalnych. Każdy zalogowany
 * użytkownik ma dostęp do API, a o dostępie do treści decydują upoważnienia
 * do folderów, sprawdzane w PermissionService.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // API bezstanowe, token w nagłówku Authorization
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Wewnętrzne przekierowanie kontenera na /error (np. po sendError(400)) nie niesie tokenu;
                // bez tej reguły denyAll() zamieniało każdy błąd 4xx/5xx w 403 z pustym ciałem.
                // Odpowiedź błędu nie zawiera danych - treść budują wyłącznie handlery wyjątków.
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> { }));
        return http.build();
    }
}
