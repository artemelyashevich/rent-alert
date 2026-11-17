CREATE TABLE IF NOT EXISTS roommate_profiles
(
    user_id         BIGINT PRIMARY KEY,

    gender          VARCHAR(16)              NOT NULL,
    gender_pref     VARCHAR[]                NOT NULL DEFAULT '{}',
    birth_year      SMALLINT,

    budget_min      INTEGER,
    budget_max      INTEGER,
    currency        VARCHAR(8),

    city            VARCHAR(255)             NOT NULL,
    move_in_from    DATE,
    move_in_to      DATE,

    lifestyle_tags  VARCHAR[]                NOT NULL DEFAULT '{}',
    schedule        VARCHAR(16),
    smoking         VARCHAR(16),
    pets            VARCHAR(16),
    guests          VARCHAR(16),
    cleanliness     SMALLINT,

    bio_submitted   TEXT,
    bio_approved    TEXT,
    bio_mod_status  VARCHAR(20)              NOT NULL DEFAULT 'PENDING',

    is_active       BOOLEAN                  NOT NULL DEFAULT TRUE,
    is_searchable   BOOLEAN                  NOT NULL DEFAULT TRUE,
    profile_version INTEGER                  NOT NULL DEFAULT 1,
    reports_count   INTEGER                  NOT NULL DEFAULT 0,
    last_digest_at  TIMESTAMP WITH TIME ZONE,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT chk_roommate_profiles_budget
        CHECK (budget_min IS NULL OR budget_max IS NULL OR budget_min <= budget_max)
);

CREATE INDEX IF NOT EXISTS idx_roommate_profiles_feed
    ON roommate_profiles (city)
    WHERE is_active AND is_searchable AND bio_mod_status = 'APPROVED';
CREATE INDEX IF NOT EXISTS idx_roommate_profiles_budget ON roommate_profiles (budget_min, budget_max);
CREATE INDEX IF NOT EXISTS idx_roommate_profiles_tags   ON roommate_profiles USING GIN (lifestyle_tags);
CREATE INDEX IF NOT EXISTS idx_roommate_profiles_pref   ON roommate_profiles USING GIN (gender_pref);

CREATE TABLE IF NOT EXISTS roommate_listings
(
    id                     BIGSERIAL PRIMARY KEY,
    author_user_id         BIGINT                   NOT NULL,
    listing_id             BIGINT,

    city                   VARCHAR(255)             NOT NULL,
    address_exact          VARCHAR(512),
    address_public         VARCHAR(512),
    district               VARCHAR(255),
    metro_station          VARCHAR(255),
    geo_lat                DOUBLE PRECISION,
    geo_lng                DOUBLE PRECISION,

    rent_share             NUMERIC(12, 2),
    currency               VARCHAR(8),
    room_count             INTEGER,
    flatmates_count        INTEGER,
    available_from         DATE,
    house_rules            VARCHAR[]                NOT NULL DEFAULT '{}',

    description_submitted  TEXT,
    description_approved   TEXT,
    description_mod_status VARCHAR(20)              NOT NULL DEFAULT 'PENDING',

    status                 VARCHAR(20)              NOT NULL DEFAULT 'DRAFT',
    gender_pref            VARCHAR[]                NOT NULL DEFAULT '{}',
    age_min                INTEGER,
    age_max                INTEGER,
    reports_count          INTEGER                  NOT NULL DEFAULT 0,

    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_roommate_listings_author ON roommate_listings (author_user_id);
CREATE INDEX IF NOT EXISTS idx_roommate_listings_feed   ON roommate_listings (status, city);

CREATE UNIQUE INDEX IF NOT EXISTS uq_roommate_listings_active_author
    ON roommate_listings (author_user_id)
    WHERE status = 'ACTIVE';

CREATE TABLE IF NOT EXISTS roommate_matches
(
    id                   BIGSERIAL PRIMARY KEY,
    scenario             VARCHAR(16)              NOT NULL,

    user_lo_id           BIGINT                   NOT NULL,
    user_hi_id           BIGINT                   NOT NULL,
    context_ref          VARCHAR(64)              NOT NULL DEFAULT '',
    roommate_listing_id  BIGINT,

    lo_decision          VARCHAR(16)              NOT NULL DEFAULT 'PENDING',
    hi_decision          VARCHAR(16)              NOT NULL DEFAULT 'PENDING',
    status               VARCHAR(20)              NOT NULL DEFAULT 'INTERESTED',
    status_version       BIGINT                   NOT NULL DEFAULT 0,

    initiator_user_id    BIGINT                   NOT NULL,
    contacts_unlocked_at TIMESTAMP WITH TIME ZONE,
    chat_ref             VARCHAR(512),
    expires_at           TIMESTAMP WITH TIME ZONE,
    lo_moved             BOOLEAN                  NOT NULL DEFAULT FALSE,
    hi_moved             BOOLEAN                  NOT NULL DEFAULT FALSE,

    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_roommate_matches_listing
        FOREIGN KEY (roommate_listing_id) REFERENCES roommate_listings (id) ON DELETE SET NULL,
    CONSTRAINT chk_roommate_matches_pair_order
        CHECK (user_lo_id < user_hi_id),
    CONSTRAINT uk_roommate_matches_pair
        UNIQUE (user_lo_id, user_hi_id, scenario, context_ref)
);

CREATE INDEX IF NOT EXISTS idx_roommate_matches_lo     ON roommate_matches (user_lo_id, status);
CREATE INDEX IF NOT EXISTS idx_roommate_matches_hi     ON roommate_matches (user_hi_id, status);
CREATE INDEX IF NOT EXISTS idx_roommate_matches_expiry ON roommate_matches (status, expires_at);

CREATE TABLE IF NOT EXISTS roommate_match_participants
(
    id         BIGSERIAL PRIMARY KEY,
    match_id   BIGINT                   NOT NULL,
    user_id    BIGINT                   NOT NULL,
    scenario   VARCHAR(16)              NOT NULL,
    role       VARCHAR(16)              NOT NULL,
    committed  BOOLEAN                  NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_roommate_participant_match
        FOREIGN KEY (match_id) REFERENCES roommate_matches (id) ON DELETE CASCADE,
    CONSTRAINT uk_roommate_match_participant
        UNIQUE (match_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_roommate_participant_user ON roommate_match_participants (user_id, scenario);

CREATE UNIQUE INDEX IF NOT EXISTS uq_roommate_participant_committed
    ON roommate_match_participants (user_id, scenario)
    WHERE committed;

CREATE TABLE IF NOT EXISTS roommate_match_events
(
    id            BIGSERIAL PRIMARY KEY,
    match_id      BIGINT                   NOT NULL,
    from_status   VARCHAR(20),
    to_status     VARCHAR(20)              NOT NULL,
    actor_user_id BIGINT,
    note          VARCHAR(512),

    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_roommate_match_events_match
        FOREIGN KEY (match_id) REFERENCES roommate_matches (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_roommate_match_events_match ON roommate_match_events (match_id, created_at);

CREATE TABLE IF NOT EXISTS roommate_viewings
(
    id                     BIGSERIAL PRIMARY KEY,
    match_id               BIGINT                   NOT NULL,
    listing_id             BIGINT,
    address                VARCHAR(512),
    viewing_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by_user_id     BIGINT                   NOT NULL,
    status                 VARCHAR(16)              NOT NULL DEFAULT 'SCHEDULED',

    listing_title          VARCHAR(1000),
    listing_street         VARCHAR(255),
    listing_metro_station  VARCHAR(255),
    listing_price          NUMERIC(12, 2),
    listing_currency       VARCHAR(10),
    listing_cover_photo_url VARCHAR(2048),

    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_roommate_viewings_match
        FOREIGN KEY (match_id) REFERENCES roommate_matches (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_roommate_viewings_match ON roommate_viewings (match_id);
CREATE INDEX IF NOT EXISTS idx_roommate_viewings_date  ON roommate_viewings (viewing_date);

CREATE TABLE IF NOT EXISTS roommate_shared_favorites
(
    match_id         BIGINT                   NOT NULL,
    listing_id       BIGINT                   NOT NULL,
    listing_status   VARCHAR(32)              NOT NULL,
    viewing_date     TIMESTAMP WITH TIME ZONE,
    added_by_user_id BIGINT                   NOT NULL,

    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    PRIMARY KEY (match_id, listing_id),

    CONSTRAINT fk_roommate_shared_fav_match
        FOREIGN KEY (match_id) REFERENCES roommate_matches (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_roommate_shared_fav_match   ON roommate_shared_favorites (match_id);
CREATE INDEX IF NOT EXISTS idx_roommate_shared_fav_listing ON roommate_shared_favorites (listing_id);

CREATE TABLE IF NOT EXISTS roommate_hidden
(
    user_id      BIGINT                   NOT NULL,
    subject_type VARCHAR(20)              NOT NULL,
    subject_id   BIGINT                   NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    PRIMARY KEY (user_id, subject_type, subject_id)
);

CREATE TABLE IF NOT EXISTS roommate_moderation_tasks
(
    id              BIGSERIAL PRIMARY KEY,
    subject_type    VARCHAR(16)              NOT NULL,
    subject_id      BIGINT                   NOT NULL,
    field           VARCHAR(64)              NOT NULL,
    submitted_text  TEXT,
    text_hash       VARCHAR(64),
    status          VARCHAR(20)              NOT NULL DEFAULT 'PENDING_MANUAL',
    reviewer_id     BIGINT,
    decision_reason VARCHAR(512),

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_roommate_moderation_status  ON roommate_moderation_tasks (status, created_at);
CREATE INDEX IF NOT EXISTS idx_roommate_moderation_subject ON roommate_moderation_tasks (subject_type, subject_id);

CREATE OR REPLACE VIEW v_roommate_calendar_events AS
SELECT
    p.user_id                                     AS user_id,
    v.id                                          AS viewing_id,
    v.match_id                                    AS match_id,
    v.listing_id                                  AS listing_id,
    v.listing_title                               AS title,
    COALESCE(v.address, v.listing_street)         AS address,
    v.listing_metro_station                       AS metro_station,
    v.listing_price                               AS price,
    v.listing_currency                            AS currency,
    v.listing_cover_photo_url                     AS cover_photo_url,
    v.viewing_date                                AS viewing_date,
    v.status                                      AS status,
    CASE WHEN m.user_lo_id = p.user_id THEN m.user_hi_id
         ELSE m.user_lo_id END                    AS counterparty_user_id,
    v.created_at                                  AS created_at,
    v.updated_at                                  AS updated_at
FROM roommate_viewings v
         JOIN roommate_matches m ON m.id = v.match_id
         JOIN roommate_match_participants p ON p.match_id = v.match_id
WHERE v.status = 'SCHEDULED';
