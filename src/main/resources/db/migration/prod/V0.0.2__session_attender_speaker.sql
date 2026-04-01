ALTER TABLE sessions
    ALTER COLUMN description DROP NOT NULL;

CREATE TABLE attender
(
    code      bigserial    NOT NULL,
    name      VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    company   VARCHAR(100),
    title     VARCHAR(100),
    email     VARCHAR(100),
    phone     VARCHAR(100),
    country   VARCHAR(5),
    CONSTRAINT pk_attender PRIMARY KEY (code)
);

CREATE INDEX idx_3646bc2ee8b2214d4cac8693c ON attender (name);

CREATE INDEX idx_b3f7cb5284c093a7bfe65731e ON attender (last_name);


-- Migrate binary photo storage to MinIO object storage.
-- photo_key stores the MinIO object key (e.g., "speakers/{uuid}", "news/{uuid}").
-- Existing BYTEA data is dropped; photos must be re-uploaded via the admin UI.

ALTER TABLE speakers
    ADD COLUMN photo_key VARCHAR(255) NULL;

ALTER TABLE news
    ADD COLUMN photo_key VARCHAR(255) NULL;

ALTER TABLE speakers
DROP COLUMN IF EXISTS photo;

ALTER TABLE news
DROP COLUMN IF EXISTS photo;