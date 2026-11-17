# Backlog projektu APSI – iteracja 1

Metodyka: **Scrum**. Sprint = **2 tygodnie**. Zespół: **7 osób**.
Backlog przechowujemy jako GitHub Issues + tablica GitHub Projects (kolumny: Backlog → To Do → In Progress → Review → Done).

Issues: [borkubja/APSI---projekt-/issues](https://github.com/borkubja/APSI---projekt-/issues) (BL-1 = #1 … BL-10 = #10).
Tablica: [borkubja/projects/11](https://github.com/users/borkubja/projects/11) (kolumny: Backlog → To Do → In Progress → Review → Done).
Milestone'y: [Sprint 1](https://github.com/borkubja/APSI---projekt-/milestone/1), [Sprint 2](https://github.com/borkubja/APSI---projekt-/milestone/2).

Zasady (uzupełniają [README.md](../README.md#zasady-pracy)):
- Tytuł issue zaczyna się od identyfikatora `BL-<n>`, np. `BL-4 Model dziedziny`. Numer issue na GitHubie może się zmienić przy przenoszeniu repozytorium, identyfikator `BL-<n>` nie.
- Gałąź: `feature/BL-<n>-krótki-opis`. W opisie PR: `Closes #<numer issue>`.
- Każde zadanie ma etykietę obszaru (`backend`, `frontend`, `baza`, `dokumentacja`, `infrastruktura`) i priorytet (`must`, `should`).
- Przypisanie (assignee) ustawiamy dopiero przy przenoszeniu zadania do *In Progress*.

## Zespół

| # | Login GitHub | Rola w Scrum | Uwagi |
| --- | --- | --- | --- |
| 1 | borkubja | Developer + Scrum Master | właściciel repozytorium `borkubja/APSI---projekt-` |
| 2 | k-miel | Developer + Product Owner | współpracownik |
| 3 | mfridayy | Developer | współpracownik |
| 4 | szymon-lukawski | Developer | współpracownik |
| 5 | magnuwam-ui | Developer | współpracownik |
| 6 | hpogorze | Developer | współpracownik |
| 7 | jlemansk1 | Developer | współpracownik |

## Cel iteracji 1

Działająca podstawa systemu: zalogowanie, wyświetlanie danych i wprowadzanie danych do bazy, wraz z dokumentacją
struktury, modelu dziedziny i przypadków użycia.

## Pozycje backlogu


| ID | Tytuł | Etykiety | Priorytet | Stan wstępny | Kryteria ukończenia |
| --- | --- | --- | --- | --- | --- |
| BL-1 | Wybór i konfiguracja narzędzia do zarządzania backlogiem | dokumentacja | must | Done po utworzeniu issues i tablicy | Wybrane narzędzie, założone issues BL-1…BL-n, tablica z kolumnami, opisany sposób pracy w README |
| BL-2 | Opracowanie i opisanie struktury systemu oraz stosu technologicznego | dokumentacja | must | Częściowo (README: struktura, stos) | Dokument architektury: warstwy (frontend, backend, baza, Keycloak, pliki), komponenty i przepływ żądania; wersje technologii zgodne z `pom.xml` i `package.json` |
| BL-3 | Instalacja i konfiguracja stosu oraz narzędzi deweloperskich | infrastruktura | must | Done | `docker compose up -d` uruchamia bazę i Keycloak; `mvn verify`, `npm run lint`, `npm run format:check`, `npm run build` przechodzą; CI zielone |
| BL-4 | Opracowanie modelu dziedziny | baza, dokumentacja | must | Częściowo (encje i migracje są, brak diagramu) | Diagram encji (Repozytorium, Folder, Dokument, Wersja, Zdarzenie obiegu, Upoważnienie, Użytkownik, Słowniki obiegu) z opisem relacji i kardynalności; zgodność z migracjami `V1`–`V3` |
| BL-5 | Identyfikacja przypadków użycia i ich opis ogólny | dokumentacja | must | Częściowo (lista UC1–UC16 w README) | Dla każdego UC: nazwa, aktor, cel, warunek wstępny, scenariusz główny, wyjątki w ogólnym zarysie; UC1–UC7 jako minimum iteracji 1 |
| BL-6 | Logowanie użytkownika (Keycloak, OIDC) | backend, frontend | must | Done | Logowanie kontem testowym, wylogowanie, 401 bez tokenu, `GET /api/me` zwraca dane zalogowanego |
| BL-7 | Wyświetlanie danych: repozytoria, foldery, dokumenty | backend, frontend | must | Done | Lista repozytoriów z uprawnieniem, drzewo folderów, lista dokumentów; brak uprawnienia → 403 |
| BL-8 | Wprowadzanie danych: zakładanie repozytorium i folderu | backend, frontend | must | Done | `POST /api/repositories`, `POST /api/folders/{id}/folders` działają z UI; dane widoczne po odświeżeniu |
| BL-9 | Wprowadzanie danych: dodawanie dokumentu | backend, frontend | must | Done | Formularz dokumentu (JSON i multipart), zapis do bazy, pierwsza wersja zapisana |
| BL-10 | Testy podstawowej funkcjonalności BL-6…BL-9 | backend | must | Częściowo (testy integracyjne są) | Testy integracyjne dla logowania, przeglądania i zapisu; przechodzą w `mvn verify` |

## Poza zakresem iteracji 1

Iteracja 2 i 3 (nowe wersje dokumentów, upoważnienia do folderów, zatwierdzanie, widoczność dokumentów) są już częściowo
zaimplementowane w kodzie. Przed przydziałem do iteracji 2 trzeba je opisać jako osobne pozycje BL-11 i kolejne.

## Plan sprintów

| Sprint | Termin | Cel | Pozycje |
| --- | --- | --- | --- |
| Sprint 1 | 19.10.2026 – 01.11.2026 (2 tygodnie) | Narzędzia i dokumentacja | BL-1, BL-2, BL-3, BL-4, BL-5 |
| Sprint 2 | 02.11.2026 – 16.11.2026 (2 tygodnie) | Funkcjonalność podstawowa i testy | BL-6, BL-7, BL-8, BL-9, BL-10 |

