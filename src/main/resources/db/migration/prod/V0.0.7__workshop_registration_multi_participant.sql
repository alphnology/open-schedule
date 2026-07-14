ALTER TABLE workshop_participant_registration
    RENAME COLUMN ticket_reference TO order_reference;

ALTER TABLE workshop_participant_registration
    DROP CONSTRAINT IF EXISTS uk_workshop_registration_ticket;

ALTER TABLE workshop_participant_registration
    ADD COLUMN reservation_id VARCHAR(128),
    ADD COLUMN ticket_id VARCHAR(128),
    ADD COLUMN ticket_public_id VARCHAR(128);

UPDATE workshop_participant_registration
SET order_reference = COALESCE(
        NULLIF(UPPER(TRIM(reservation_short_code)), ''),
        NULLIF(UPPER(TRIM(order_reference)), ''),
        order_reference
    );

CREATE UNIQUE INDEX IF NOT EXISTS uk_workshop_registration_ticket_public_id
    ON workshop_participant_registration (event_slug, ticket_public_id)
    WHERE ticket_public_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_workshop_registration_order_reference
    ON workshop_participant_registration (order_reference);

CREATE INDEX IF NOT EXISTS idx_workshop_registration_ticket_public_id
    ON workshop_participant_registration (ticket_public_id);
