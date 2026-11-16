-- Iteracja 1: schemat zgodny z modelem dziedziny po ustaleniach z prowadzącym.
-- Płaska hierarchia użytkowników, dostęp przez upoważnienia do folderów.
-- Kolejne iteracje dodają: metadane (metadata_field, metadata_value), baseline, subskrypcje.

CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    keycloak_id   VARCHAR(64)  NOT NULL UNIQUE,   -- "sub" z tokenu
    username      VARCHAR(120) NOT NULL UNIQUE,
    first_name    VARCHAR(120),
    last_name     VARCHAR(120),
    email         VARCHAR(255)
);

-- Repozytorium = folder najwyższego poziomu z własnymi słownikami.
CREATE TABLE repository (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(200) NOT NULL UNIQUE,
    description  TEXT,
    owner_id     BIGINT NOT NULL REFERENCES app_user(id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE folder (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(200) NOT NULL,
    repository_id  BIGINT NOT NULL REFERENCES repository(id),
    parent_id      BIGINT REFERENCES folder(id),   -- NULL = folder główny repozytorium
    UNIQUE (repository_id, parent_id, name)
);

-- Upoważnienie użytkownika do folderu; dziedziczone przez podfoldery.
CREATE TABLE folder_permission (
    id          BIGSERIAL PRIMARY KEY,
    folder_id   BIGINT NOT NULL REFERENCES folder(id),
    user_id     BIGINT NOT NULL REFERENCES app_user(id),
    level       VARCHAR(10) NOT NULL CHECK (level IN ('READ', 'WRITE')),
    granted_by  BIGINT REFERENCES app_user(id),
    granted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (folder_id, user_id)
);

-- Słownik ról i działań obiegu, osobny dla każdego repozytorium.
CREATE TABLE role_definition (
    id             BIGSERIAL PRIMARY KEY,
    repository_id  BIGINT NOT NULL REFERENCES repository(id),
    name           VARCHAR(80) NOT NULL,
    UNIQUE (repository_id, name)
);

CREATE TABLE action_type (
    id          BIGSERIAL PRIMARY KEY,
    role_id     BIGINT NOT NULL REFERENCES role_definition(id),
    name        VARCHAR(80) NOT NULL,
    -- START: utworzenie (automatyczne), MIDDLE: przed zatwierdzeniem,
    -- END: zatwierdzenie (blokuje wersję), AFTER_END: po zatwierdzeniu (np. odbiór)
    stage       VARCHAR(10) NOT NULL CHECK (stage IN ('START', 'MIDDLE', 'END', 'AFTER_END')),
    sort_order  INT NOT NULL DEFAULT 0
);

CREATE TABLE document (
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    folder_id    BIGINT NOT NULL REFERENCES folder(id),
    author_id    BIGINT NOT NULL REFERENCES app_user(id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted      BOOLEAN NOT NULL DEFAULT FALSE
);

-- Drzewo wersji: parent_version_id wskazuje wersję-rodzica (NULL = korzeń).
CREATE TABLE document_version (
    id                 BIGSERIAL PRIMARY KEY,
    document_id        BIGINT NOT NULL REFERENCES document(id),
    parent_version_id  BIGINT REFERENCES document_version(id),
    version_label      VARCHAR(30) NOT NULL,
    change_note        TEXT,
    approved           BOOLEAN NOT NULL DEFAULT FALSE,
    file_key           VARCHAR(255),
    sha256             VARCHAR(64),
    size_bytes         BIGINT,
    mime_type          VARCHAR(120),
    author_id          BIGINT NOT NULL REFERENCES app_user(id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (document_id, version_label)
);

-- Historia obiegu wersji: kto, jakie działanie, kiedy.
CREATE TABLE version_event (
    id              BIGSERIAL PRIMARY KEY,
    version_id      BIGINT NOT NULL REFERENCES document_version(id),
    action_type_id  BIGINT NOT NULL REFERENCES action_type(id),
    user_id         BIGINT NOT NULL REFERENCES app_user(id),
    comment         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_folder_repository ON folder(repository_id);
CREATE INDEX idx_permission_user ON folder_permission(user_id);
CREATE INDEX idx_document_folder ON document(folder_id);
CREATE INDEX idx_version_document ON document_version(document_id);
CREATE INDEX idx_event_version ON version_event(version_id);
