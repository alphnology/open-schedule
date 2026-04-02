CREATE TABLE mail_settings
(
    code                  BIGSERIAL PRIMARY KEY,
    singleton_key         VARCHAR(32)  NOT NULL UNIQUE,
    outbound_enabled      BOOLEAN      NOT NULL DEFAULT FALSE,
    provider_type         VARCHAR(20)  NOT NULL,
    security_mode         VARCHAR(20)  NOT NULL,
    authentication_enabled BOOLEAN     NOT NULL DEFAULT TRUE,
    host                  VARCHAR(255),
    port                  INTEGER,
    username              VARCHAR(255),
    encrypted_secret      TEXT,
    from_address          VARCHAR(255),
    from_name             VARCHAR(255),
    postal_base_url       VARCHAR(255),
    postal_api_key_header VARCHAR(255),
    ssl_trust             VARCHAR(255),
    connection_timeout_ms INTEGER,
    read_timeout_ms       INTEGER,
    test_recipient        VARCHAR(255)
);
