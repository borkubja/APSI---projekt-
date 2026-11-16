# APSI – Repozytorium dokumentów

Moduł 1 zintegrowanego systemu wspomagającego zarządzanie pracownikami i projektami w firmie IT:
**repozytorium dokumentów z obiegiem i drzewiastym wersjonowaniem**. Hierarchia użytkowników jest płaska –
o dostępie decydują upoważnienia do folderów (odczyt albo zapis), dziedziczone przez podfoldery.

Stan: **iteracja 1** (logowanie, przeglądanie, zakładanie repozytoriów i folderów, dodawanie dokumentów) wraz z częścią
funkcji iteracji 2–3 zrealizowaną z wyprzedzeniem (pliki i nowe wersje, upoważnienia, zatwierdzanie).

## Wymagania

| Narzędzie | Wersja | Sprawdzenie |
| --- | --- | --- |
| Docker z Docker Compose v2 | aktualna (Docker Desktop lub Engine) | `docker compose version` |
| JDK | 21 (np. Temurin) | `java -version` |
| Maven | 3.9 lub nowszy | `mvn -v` |
| Node.js (z npm) | 20, 22 lub nowszy LTS | `node -v` |
| Git | aktualna | `git --version` |

Docker jest potrzebny **także do testów backendu** (Testcontainers uruchamia w nim PostgreSQL).
Docker Engine 29 wymaga Testcontainers 1.21.4+ – wersja jest już ustawiona w `backend/pom.xml`.

Windows: `winget install EclipseAdoptium.Temurin.21.JDK`, `winget install Docker.DockerDesktop`,
`winget install OpenJS.NodeJS.LTS`. Maven nie ma paczki w winget – pobierz zip z
<https://maven.apache.org/download.cgi>, rozpakuj, dodaj `bin` do `PATH` i ustaw `JAVA_HOME`.
Polecenia poniżej działają w Git Bash, PowerShell i terminalu Linux/macOS.

## Konfiguracja

Projekt działa bez żadnej konfiguracji (wartości domyślne z tabeli). Jeśli któryś port jest zajęty albo chcesz zmienić
dane logowania do bazy, skopiuj plik wzorcowy i edytuj tylko potrzebne linie:

```bash
cp .env.example .env
```

Plik `.env` (nie jest w repozytorium) czytają: `docker compose`, backend (przy starcie z katalogu `backend/`)
i frontend (Vite). Zmienne środowiskowe ustawione w powłoce mają pierwszeństwo.

| Zmienna | Domyślnie | Znaczenie |
| --- | --- | --- |
| `POSTGRES_PORT` | 5432 | Port bazy na Twoim komputerze |
| `KEYCLOAK_PORT` | 8180 | Port Keycloak |
| `BACKEND_PORT` | 8080 | Port backendu |
| `FRONTEND_PORT` | 5173 | Port serwera deweloperskiego frontendu |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | apsi | Baza i dane logowania (tylko do rozwoju) |
| `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD` | admin | Konto administratora Keycloak |
| `POSTGRES_HOST` | localhost | Host bazy (backend) |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8180/realms/apsi` | Adres wystawcy tokenów (backend) |
| `STORAGE_DIR` | storage | Katalog plików wersji (backend) |

Dane logowania w repozytorium to wyłącznie wartości deweloperskie. Nie używaj ich poza lokalnym środowiskiem.

## Uruchomienie krok po kroku

```bash
# 1. Pobierz kod
git clone <adres-repozytorium> apsi-repo
cd apsi-repo

# 2. (opcjonalnie) własna konfiguracja portów
cp .env.example .env

# 3. Baza danych i Keycloak
docker compose up -d
```

Pierwszy start Keycloak trwa **30–60 sekund** (importuje realm z kontami testowymi). Poczekaj, aż odpowie:

```bash
curl http://localhost:8180/realms/apsi     # zwraca JSON z "realm":"apsi", gdy Keycloak jest gotowy
```

```bash
# 4. Backend (nowy terminal) - http://localhost:8080
cd backend
mvn spring-boot:run

# 5. Frontend (kolejny terminal) - http://localhost:5173
cd frontend
npm install
npm run dev
```

Otwórz <http://localhost:5173> i zaloguj się kontem z tabeli poniżej.

### Inicjalizacja bazy

Nie robisz nic ręcznie: przy starcie backendu **Flyway** tworzy schemat (`V1`), dane demo (`V2` – repozytorium
„Bank – system kredytowy”, foldery, dokumenty, w tym równoległe wersje 1.1 i 1.0.1) i rozszerzenia (`V3`).
Aby zacząć od zera: `docker compose down -v` i ponownie `docker compose up -d`
(usuwa bazę i realm Keycloak; katalog `backend/storage/` z plikami usuń ręcznie).

### Adresy

| Co | Adres |
| --- | --- |
| Aplikacja | <http://localhost:5173> |
| API / Swagger UI | <http://localhost:8080> / <http://localhost:8080/swagger-ui.html> |
| Konsola Keycloak | <http://localhost:8180> (admin / admin) |

## Konta testowe

Hasło wszystkich kont: `haslo`.

| Login | Imię i nazwisko | Upoważnienie w repozytorium demo | Co widzi |
| --- | --- | --- | --- |
| `jan.kowalski` | Jan Kowalski | zapis (właściciel) | wszystko; dodaje dokumenty i foldery, zatwierdza |
| `anna.nowak` | Anna Nowak | zapis | wszystko; dodaje dokumenty i foldery, zatwierdza |
| `piotr.wisniewski` | Piotr Wiśniewski | odczyt | dokumenty bez formularzy zapisu |
| `ewa.zielinska` | Ewa Zielińska | brak | pustą listę repozytoriów (może założyć własne) |

## Testy i jakość kodu

```bash
# Backend: Checkstyle + testy jednostkowe i integracyjne (wymaga uruchomionego Dockera)
cd backend
mvn verify

# Frontend: lint, kontrola formatowania i build (sprawdzenie typów)
cd frontend
npm run lint
npm run format:check       # popraw automatycznie: npm run format
npm run build
```

Testy integracyjne uruchamiają PostgreSQL w kontenerze, więc **nie wymagają** działającego `docker compose`
ani Keycloak i nie zmieniają Twojej bazy deweloperskiej. Pierwsze uruchomienie pobiera obraz `postgres:16`.
To samo wykonuje CI w GitHub Actions (`.github/workflows/ci.yml`).

## Rozwiązywanie problemów

| Objaw | Przyczyna i rozwiązanie |
| --- | --- |
| `Port 8080 was already in use` (backend) albo `port is already allocated` (Docker) | Inny program używa portu. Znajdź go: Windows `netstat -ano \| findstr :8080`, Linux/macOS `lsof -i :8080`. Zakończ go **albo** zmień port w `.env` (np. `BACKEND_PORT=8081`, `POSTGRES_PORT=5433`) i uruchom ponownie. Zmiana `KEYCLOAK_PORT` wymaga też `docker compose up -d` po zmianie. |
| Vite: `Port 5173 is already in use` | Ustaw `FRONTEND_PORT` w `.env` (Vite jest uruchamiany z `strictPort`, więc nie wybierze innego sam). |
| Po zalogowaniu zawsze ekran logowania lub 401 | Keycloak jeszcze startuje (sprawdź `curl http://localhost:8180/realms/apsi`) albo backend ma inny `KEYCLOAK_ISSUER_URI` niż adres, pod którym działa Keycloak. |
| Zmieniłem `keycloak/realm-apsi.json`, a nic się nie zmienia | Realm jest importowany tylko raz: `docker compose down -v`, potem `docker compose up -d`. |
| `Could not find a valid Docker environment` w testach | Uruchom Docker Desktop / demona Dockera. |
| `Flyway ... validate failed` lub błędy schematu po zmianie kodu | Masz bazę ze starszej wersji migracji: `docker compose down -v` i ponowny start. |
| `JAVA_HOME is not defined correctly` | Ustaw `JAVA_HOME` na katalog JDK 21. |
| Kontener Postgres jest `unhealthy` | `docker compose logs postgres`; najczęściej zajęty port lub uszkodzony wolumen (`docker compose down -v`). |

## API

Pełny opis i próbne wywołania: Swagger UI. Wszystkie ścieżki wymagają tokenu JWT (brak → 401);
brak upoważnienia do folderu → 403; błędy mają ciało `application/problem+json`.

| Metoda | Ścieżka | Wymagane upoważnienie | UC |
| --- | --- | --- | --- |
| GET | `/api/me` | zalogowany | UC1 |
| GET | `/api/users` | zalogowany | UC6 |
| GET | `/api/repositories` | zalogowany (lista tylko z upoważnieniem) | UC2 |
| POST | `/api/repositories` | zalogowany (twórca = właściciel, zapis) | UC3 |
| GET | `/api/repositories/{id}/folders` | odczyt (lista tylko z upoważnieniem) | UC2 |
| POST | `/api/folders/{id}/folders` | zapis | UC3 |
| GET, PUT | `/api/folders/{id}/permissions` | zapis | UC6 |
| DELETE | `/api/folders/{id}/permissions/{userId}` | zapis | UC6 |
| GET | `/api/folders/{id}/documents` | odczyt (+ widoczność dokumentu) | UC2 |
| POST | `/api/folders/{id}/documents` (JSON lub multipart) | zapis | UC7 |
| PUT | `/api/documents/{id}/visibility` | autor lub zapis | UC16 |
| GET | `/api/documents/{id}/versions` | odczyt | UC5 |
| POST | `/api/documents/{id}/versions` (multipart) | zapis | UC4 |
| POST | `/api/versions/{id}/approve` | zapis | UC11 |
| GET | `/api/versions/{id}/file` | odczyt | UC15 |

## Struktura repozytorium

```
docker-compose.yml        PostgreSQL i Keycloak
.env.example              wzór konfiguracji (porty, dane logowania)
keycloak/realm-apsi.json  realm, konta testowe (stałe id = keycloak_id w bazie)
backend/                  Spring Boot 3.5, Java 21, Maven
  pl.apsi.repo.config       bezpieczeństwo, obsługa błędów
  pl.apsi.repo.repository   repozytoria, foldery, upoważnienia, słownik obiegu
  pl.apsi.repo.document     dokumenty, wersje, zdarzenia obiegu, pliki
  pl.apsi.repo.user         użytkownik z tokenu, lista użytkowników
  resources/db/migration    migracje Flyway
  src/test                  testy jednostkowe i integracyjne
frontend/                 React 18 + TypeScript + Vite
.github/workflows/ci.yml  build, lint i testy
```

## Zasady pracy

- Gałąź na zadanie: `feature/BL-14-formularz-dokumentu`, PR do `main`, co najmniej jedno review i zielone CI.
- W opisie PR: `Closes #<numer issue>` – zadanie zamknie się i przejdzie do Done.
- Schemat bazy zmieniamy tylko nową migracją `V<n>__opis.sql`, nigdy edycją starej.
