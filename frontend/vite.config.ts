import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// Porty czytamy z pliku .env w katalogu głównym repozytorium (wzór: .env.example); bez niego obowiązują domyślne.
// /api przekazujemy do backendu - przeglądarka widzi jedno źródło, więc CORS nie jest potrzebny.
export default defineConfig(({ mode }) => {
  const env = { ...loadEnv(mode, '..', ''), ...process.env };
  const backendPort = env.BACKEND_PORT ?? '8080';
  return {
    plugins: [react()],
    // Udostępnia przeglądarce tylko te zmienne z .env: KEYCLOAK_PORT (adres logowania) i VITE_* (nadpisania).
    envDir: '..',
    envPrefix: ['VITE_', 'KEYCLOAK_PORT'],
    server: {
      port: Number(env.FRONTEND_PORT ?? 5173),
      strictPort: true,
      proxy: {
        '/api': `http://localhost:${backendPort}`,
      },
    },
  };
});
