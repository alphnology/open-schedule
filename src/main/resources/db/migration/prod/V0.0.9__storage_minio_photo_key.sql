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
