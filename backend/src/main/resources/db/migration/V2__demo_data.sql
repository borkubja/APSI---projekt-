-- Dane demonstracyjne iteracji 1.
-- keycloak_id muszą zgadzać się z polem "id" użytkowników w keycloak/realm-apsi.json.

INSERT INTO app_user (id, keycloak_id, username, first_name, last_name, email) VALUES
    (1, '0f1c2a3b-0000-4000-8000-000000000001', 'jan.kowalski',     'Jan',   'Kowalski',    'jan.kowalski@apsi.local'),
    (2, '0f1c2a3b-0000-4000-8000-000000000002', 'anna.nowak',       'Anna',  'Nowak',       'anna.nowak@apsi.local'),
    (3, '0f1c2a3b-0000-4000-8000-000000000003', 'piotr.wisniewski', 'Piotr', 'Wiśniewski',  'piotr.wisniewski@apsi.local'),
    (4, '0f1c2a3b-0000-4000-8000-000000000004', 'ewa.zielinska',    'Ewa',   'Zielińska',   'ewa.zielinska@apsi.local');
SELECT setval('app_user_id_seq', 4);

INSERT INTO repository (id, name, description, owner_id) VALUES
    (1, 'Bank – system kredytowy', 'Dokumentacja projektu systemu obsługi kredytów', 1);
SELECT setval('repository_id_seq', 1);

-- Folder główny (parent NULL) i podfoldery.
INSERT INTO folder (id, name, repository_id, parent_id) VALUES
    (1, 'Bank – system kredytowy', 1, NULL),
    (2, 'Wymagania', 1, 1),
    (3, 'Projekt',   1, 1),
    (4, 'Testy',     1, 1);
SELECT setval('folder_id_seq', 4);

-- Upoważnienia do folderu głównego (dziedziczone przez podfoldery).
-- ewa.zielinska celowo nie ma żadnego upoważnienia.
INSERT INTO folder_permission (folder_id, user_id, level, granted_by) VALUES
    (1, 1, 'WRITE', 1),
    (1, 2, 'WRITE', 1),
    (1, 3, 'READ',  1);

-- Domyślny słownik ról i działań repozytorium (ten sam tworzy RepoService dla nowych repozytoriów).
INSERT INTO role_definition (id, repository_id, name) VALUES
    (1, 1, 'Autor'), (2, 1, 'Edytor'), (3, 1, 'Kontroler'), (4, 1, 'Recenzent'),
    (5, 1, 'Zatwierdzający'), (6, 1, 'Odbierający');
SELECT setval('role_definition_id_seq', 6);

INSERT INTO action_type (id, role_id, name, stage, sort_order) VALUES
    (1, 1, 'Utworzyłem',      'START',     1),
    (2, 2, 'Edytowałem',      'MIDDLE',    2),
    (3, 3, 'Skontrolowałem',  'MIDDLE',    3),
    (4, 4, 'Zrecenzowałem',   'MIDDLE',    4),
    (5, 4, 'Zgłosiłem uwagi', 'MIDDLE',    5),
    (6, 5, 'Zatwierdziłem',   'END',       6),
    (7, 6, 'Odebrałem',       'AFTER_END', 7);
SELECT setval('action_type_id_seq', 7);

INSERT INTO document (id, title, description, folder_id, author_id) VALUES
    (1, 'Specyfikacja wymagań', 'Wymagania funkcjonalne i niefunkcjonalne', 2, 1),
    (2, 'Słownik pojęć',        'Słownik dziedziny bankowej',               2, 2),
    (3, 'Architektura systemu', 'Opis architektury i wdrożenia',            3, 1);
SELECT setval('document_id_seq', 3);

-- Specyfikacja: 1.0 -> dwie równoległe wersje 1.1 i 1.1.1 (gałąź), 1.0 zatwierdzona.
INSERT INTO document_version (id, document_id, parent_version_id, version_label, change_note, approved, author_id) VALUES
    (1, 1, NULL, '1.0',   'Pierwsza wersja',            TRUE,  1),
    (2, 1, 1,    '1.1',   'Poprawki po recenzji',       FALSE, 1),
    (3, 1, 1,    '1.0.1', 'Wariant dla klienta B',      FALSE, 2),
    (4, 2, NULL, '0.1',   'Szkic',                      FALSE, 2),
    (5, 3, NULL, '0.1',   'Szkic',                      FALSE, 1);
SELECT setval('document_version_id_seq', 5);

INSERT INTO version_event (version_id, action_type_id, user_id, comment) VALUES
    (1, 1, 1, NULL),
    (1, 4, 2, 'Brak uwag'),
    (1, 6, 1, NULL),
    (2, 1, 1, NULL),
    (2, 2, 2, 'Uzupełnione wymagania WF.03'),
    (3, 1, 2, NULL),
    (4, 1, 2, NULL),
    (5, 1, 1, NULL);
