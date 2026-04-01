--   #####   #######  ######   #     #   #####   #######  #     #  ######   #######
--  #     #     #     #     #  #     #  #     #     #     #     #  #     #  #
--  #           #     #     #  #     #  #           #     #     #  #     #  #
--   #####      #     ######   #     #  #           #     #     #  ######   #####
--        #     #     #   #    #     #  #           #     #     #  #   #    #
--  #     #     #     #    #   #     #  #     #     #     #     #  #    #   #
--   #####      #     #     #   #####    #####      #      #####   #     #  #######
CREATE
EXTENSION IF NOT EXISTS unaccent;


CREATE TABLE users
(
    code              bigserial    NOT NULL,
    username          varchar(100) NOT NULL,
    "password"        varchar(250) NOT NULL,
    "name"            varchar(100) NOT NULL,
    phone             varchar(30) NULL,
    roles             varchar(255) NOT NULL,
    favorite_sessions jsonb NULL,
    last_login_ts     timestamptz(6) NULL,
    "locked"          bool         NOT NULL,
    one_log_pwd       bool         NOT NULL,
    CONSTRAINT ukr43af9ap4edm43mmtq01oddj6 UNIQUE (username),
    CONSTRAINT users_pkey PRIMARY KEY (code)
);
CREATE INDEX idxccp93091nwrdtqfofn36fxx7y ON users USING btree (username, name);



CREATE TABLE tracks
(
    code   bigserial    NOT NULL,
    "name" varchar(100) NOT NULL,
    color  varchar(20) NULL,
    CONSTRAINT tracks_pkey PRIMARY KEY (code),
    CONSTRAINT uk2q4x1t51xpqqquojnnti17s68 UNIQUE (name)
);
CREATE INDEX idx2q4x1t51xpqqquojnnti17s68 ON tracks USING btree (name);



CREATE TABLE tags
(
    code   bigserial    NOT NULL,
    "name" varchar(100) NOT NULL,
    color  varchar(20) NULL,
    CONSTRAINT tags_pkey PRIMARY KEY (code),
    CONSTRAINT ukt48xdq560gs3gap9g7jg36kgc UNIQUE (name)
);
CREATE INDEX idxt48xdq560gs3gap9g7jg36kgc ON tags USING btree (name);



CREATE TABLE sponsor_categories
(
    code   bigserial    NOT NULL,
    "name" varchar(100) NOT NULL,
    CONSTRAINT sponsor_categories_pkey PRIMARY KEY (code),
    CONSTRAINT uk5n0u1y20ekv9ocq09alsm7ove UNIQUE (name)
);
CREATE INDEX idx5n0u1y20ekv9ocq09alsm7ove ON sponsor_categories USING btree (name);



CREATE TABLE sponsors
(
    code        bigserial     NOT NULL,
    "name"      varchar(100)  NOT NULL,
    description varchar(3000) NOT NULL,
    category    int8          NOT NULL,
    photo_url   varchar(200) NULL,
    CONSTRAINT sponsors_pkey PRIMARY KEY (code)
);
CREATE INDEX idxnsgh4yktbhfbenpbelafihec6 ON sponsors USING btree (name);
ALTER TABLE sponsors
    ADD CONSTRAINT fka77nnm0lafahy0ymwcn8k4thm FOREIGN KEY (category) REFERENCES sponsor_categories (code);



CREATE TABLE speakers
(
    code       bigserial    NOT NULL,
    "name"     varchar(100) NOT NULL,
    title      varchar(100) NULL,
    company    varchar(100) NULL,
    email      VARCHAR(100) NULL,
    phone      VARCHAR(100) NULL,
    country    varchar(5) NULL,
    bio        varchar(3000) NULL,
    networking jsonb NULL,
    photo      BYTEA NULL,
    CONSTRAINT speakers_pkey PRIMARY KEY (code)
);
CREATE INDEX idxnl722pbt2jls8awi56cyqwlud ON speakers USING btree (name);


CREATE TABLE events
(
    code       bigserial    NOT NULL,
    "name"     varchar(100) NOT NULL,
    start_date date         NOT NULL,
    end_date   date         NOT NULL,
    "location" varchar(100) NULL,
    time_zone  varchar(50)  NOT NULL,
    CONSTRAINT events_pkey PRIMARY KEY (code),
    CONSTRAINT ukfn2r8jg0sm5v6vhoa7yqw55vy UNIQUE (name)
);
CREATE INDEX idxfn2r8jg0sm5v6vhoa7yqw55vy ON events USING btree (name);



CREATE TABLE rooms
(
    code     bigserial    NOT NULL,
    "name"   varchar(100) NOT NULL,
    capacity int4         NOT NULL,
    color    varchar(20) NULL,
    CONSTRAINT rooms_pkey PRIMARY KEY (code),
    CONSTRAINT uk1kuqhbfxed2e8t571uo82n545 UNIQUE (name)
);
CREATE INDEX idx1kuqhbfxed2e8t571uo82n545 ON rooms USING btree (name);



CREATE TABLE sessions
(
    code        bigserial     NOT NULL,
    title       varchar(300)  NOT NULL,
    description varchar(3000) NOT NULL,
    start_time  timestamp(6)  NOT NULL,
    end_time    timestamp(6)  NOT NULL,
    "level"     varchar(2) NULL,
    "language"  varchar(5) NULL,
    "type"      varchar(2)    NOT NULL,
    room        int8 NULL,
    track       int8 NULL,
    CONSTRAINT sessions_pkey PRIMARY KEY (code)
);
CREATE INDEX idx254bj2iqk8arcr891edstd5k ON sessions USING btree (start_time, end_time);
CREATE INDEX idx44bg08b0ggr467wmxxxdckyc7 ON sessions USING btree (type);
CREATE INDEX idxbowi71siaa5iqdaiknpv3fnq5 ON sessions USING btree (title);
ALTER TABLE sessions
    ADD CONSTRAINT fk5djlw6klbg25geb2y0r562uvd FOREIGN KEY (track) REFERENCES tracks (code);
ALTER TABLE sessions
    ADD CONSTRAINT fkay9shqjwddnubjdettme2u7ar FOREIGN KEY (room) REFERENCES rooms (code);



CREATE TABLE session_speaker
(
    session_code int8 NOT NULL,
    speaker_id   int8 NOT NULL,
    CONSTRAINT session_speaker_pkey PRIMARY KEY (session_code, speaker_id)
);
ALTER TABLE session_speaker
    ADD CONSTRAINT fk68r1f8thwy0vuwtx38hth6d9c FOREIGN KEY (session_code) REFERENCES sessions (code);
ALTER TABLE session_speaker
    ADD CONSTRAINT fknfg2ijll5eq0r3c9gxxi2attk FOREIGN KEY (speaker_id) REFERENCES speakers (code);


CREATE TABLE session_tag
(
    session_code int8 NOT NULL,
    tag_id       int8 NOT NULL,
    CONSTRAINT session_tag_pkey PRIMARY KEY (session_code, tag_id)
);
ALTER TABLE session_tag
    ADD CONSTRAINT fk1umft5bpqh48csjyy3d38cw6b FOREIGN KEY (session_code) REFERENCES sessions (code);
ALTER TABLE session_tag
    ADD CONSTRAINT fkklpct7fbayvns51maq0uoyplw FOREIGN KEY (tag_id) REFERENCES tags (code);


CREATE TABLE session_ratings
(
    code      bigserial NOT NULL,
    "session" int8      NOT NULL,
    users     int8      NOT NULL,
    score     int4      NOT NULL,
    "comment" varchar(200) NULL,
    CONSTRAINT session_ratings_pkey PRIMARY KEY (code),
    CONSTRAINT session_ratings_score_check CHECK (((score <= 5) AND (score >= 1))),
    CONSTRAINT ukr2339nx57a8nvwxxgmkq4n2m6 UNIQUE (session, users)
);
CREATE INDEX idxr2339nx57a8nvwxxgmkq4n2m6 ON session_ratings USING btree (session, users);
ALTER TABLE session_ratings
    ADD CONSTRAINT fk4ahrf4so3uxserlagyqs5592y FOREIGN KEY (users) REFERENCES users (code);
ALTER TABLE session_ratings
    ADD CONSTRAINT fk9h0tiop6s3tmwbt2i1hm44vlo FOREIGN KEY ("session") REFERENCES sessions (code);


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


--
-- -- --  ######      #     #######     #
-- -- --  #     #    # #       #       # #
-- -- --  #     #   #   #      #      #   #
-- -- --  #     #  #     #     #     #     #
-- -- --  #     #  #######     #     #######
-- -- --  #     #  #     #     #     #     #
-- -- --  ######   #     #     #     #     #
--

INSERT INTO users (username, "password", "name", roles, phone, last_login_ts, locked, one_log_pwd)
VALUES ('me@fredpena.dev', '$2a$10$5iKop40Zw8srnTmvap0tXuiVSHgg6N4Jbgjy6LNmfsMBZq2ETT3HO',
        'Freddy Pena', 'ADMIN', '(829) 820 2626', NULL, FALSE, FALSE);






