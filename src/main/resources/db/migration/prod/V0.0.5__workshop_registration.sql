CREATE TABLE workshop_registration_settings
(
    code                        BIGSERIAL PRIMARY KEY,
    singleton_key               VARCHAR(32)  NOT NULL UNIQUE,
    enabled                     BOOLEAN      NOT NULL DEFAULT FALSE,
    active                      BOOLEAN      NOT NULL DEFAULT TRUE,
    alfio_base_url              VARCHAR(255),
    event_slug                  VARCHAR(100),
    encrypted_token             TEXT,
    public_message              TEXT,
    participant_workshop_limit  INTEGER      NOT NULL DEFAULT 1,
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workshop_participant_registration
(
    code                    BIGSERIAL PRIMARY KEY,
    event_slug              VARCHAR(100)  NOT NULL,
    ticket_reference        VARCHAR(128)  NOT NULL,
    reservation_short_code  VARCHAR(64),
    attendee_name           VARCHAR(255)  NOT NULL,
    attendee_email          VARCHAR(255)  NOT NULL,
    session_code            BIGINT        NOT NULL REFERENCES sessions (code) ON DELETE CASCADE,
    status                  VARCHAR(32)   NOT NULL,
    alfio_reservation_status VARCHAR(64),
    alfio_payload_json      TEXT,
    validated_at            TIMESTAMP     NOT NULL,
    registered_at           TIMESTAMP     NOT NULL,
    updated_at              TIMESTAMP     NOT NULL,
    CONSTRAINT uk_workshop_registration_ticket UNIQUE (event_slug, ticket_reference)
);

CREATE INDEX idx_workshop_registration_session_code
    ON workshop_participant_registration (session_code);

CREATE INDEX idx_workshop_registration_email
    ON workshop_participant_registration (attendee_email);

CREATE INDEX idx_workshop_registration_name
    ON workshop_participant_registration (attendee_name);
