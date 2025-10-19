CREATE TABLE news
(
    code         bigserial    NOT NULL,
    title        VARCHAR(255) NOT NULL,
    content      TEXT         NOT NULL,
    author       int8         NOT NULL,
    published_at timestamptz(6)             NOT NULL,
    photo        BYTEA NULL,
    version      INTEGER      NOT NULL,
    CONSTRAINT pk_news PRIMARY KEY (code)
);

CREATE INDEX idx_16e4a896086dd1a25250ee6e7 ON news (title);

CREATE INDEX idx_7e77656b66f71b253d52573c0 ON news (published_at);

ALTER TABLE news
    ADD CONSTRAINT FK_NEWS_ON_AUTHOR FOREIGN KEY (author) REFERENCES users (code);