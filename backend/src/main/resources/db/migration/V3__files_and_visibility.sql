-- Iteracja 2: nazwa pliku wersji oraz ograniczenie widoczności dokumentu do wybranych osób.
ALTER TABLE document_version ADD COLUMN file_name VARCHAR(255);

-- restricted = TRUE: dokument widzą tylko autor, osoby z zapisem do folderu i osoby z document_viewer.
ALTER TABLE document ADD COLUMN restricted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE document_viewer (
    document_id  BIGINT NOT NULL REFERENCES document(id),
    user_id      BIGINT NOT NULL REFERENCES app_user(id),
    PRIMARY KEY (document_id, user_id)
);
