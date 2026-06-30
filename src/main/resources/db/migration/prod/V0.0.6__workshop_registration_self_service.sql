ALTER TABLE workshop_registration_settings
    ADD COLUMN IF NOT EXISTS allow_attendee_workshop_change BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE workshop_registration_settings
    ADD COLUMN IF NOT EXISTS show_public_menu_entry BOOLEAN NOT NULL DEFAULT FALSE;
