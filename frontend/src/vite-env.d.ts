/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Pełny adres Keycloak (opcjonalne nadpisanie), np. http://localhost:8180. */
  readonly VITE_KEYCLOAK_URL?: string;
  /** Port Keycloak z pliku .env w katalogu głównym repozytorium. */
  readonly KEYCLOAK_PORT?: string;
}
