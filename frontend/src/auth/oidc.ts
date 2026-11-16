import type { AuthProviderProps } from 'react-oidc-context';
import { WebStorageStateStore } from 'oidc-client-ts';

// Adres Keycloak: VITE_KEYCLOAK_URL (pełny adres) albo localhost:KEYCLOAK_PORT z pliku .env (domyślnie 8180).
const keycloakUrl = import.meta.env.VITE_KEYCLOAK_URL ?? `http://localhost:${import.meta.env.KEYCLOAK_PORT ?? '8180'}`;

// Konfiguracja logowania przez Keycloak (OIDC, Authorization Code + PKCE).
export const oidcConfig: AuthProviderProps = {
  authority: `${keycloakUrl}/realms/apsi`,
  client_id: 'apsi-frontend',
  redirect_uri: window.location.origin + '/',
  post_logout_redirect_uri: window.location.origin + '/',
  scope: 'openid profile email',
  userStore: new WebStorageStateStore({ store: window.sessionStorage }),
  // po powrocie z Keycloak usuwamy ?code=... z paska adresu
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, window.location.pathname);
  },
};
