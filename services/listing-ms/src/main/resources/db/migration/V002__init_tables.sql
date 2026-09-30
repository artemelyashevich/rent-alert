-- public.listing_status definition

-- Drop table

-- DROP TABLE public.listing_status;

CREATE TABLE IF NOT EXISTS public.listing_status
(
    id
    bigserial
    NOT
    NULL,
    "name"
    varchar
(
    50
) NOT NULL,
    color varchar
(
    7
) NOT NULL,
    description varchar
(
    255
) NOT NULL,
    created_at timestamptz DEFAULT now
(
) NOT NULL,
    updated_at timestamptz DEFAULT now
(
) NOT NULL,
    shows_in_calendar bool DEFAULT false NOT NULL,
    CONSTRAINT listing_status_name_key UNIQUE
(
    name
),
    CONSTRAINT listing_status_pkey PRIMARY KEY
(
    id
)
    );

-- public.listings definition

-- Drop table

-- DROP TABLE public.listings;

CREATE TABLE IF NOT EXISTS public.listings
(
    id
    bigserial
    NOT
    NULL,
    "source"
    varchar
(
    20
) NOT NULL,
    source_id varchar
(
    255
) NOT NULL,
    source_url varchar
(
    2048
) NULL,
    title varchar
(
    1000
) NULL,
    description text NULL,
    street varchar
(
    255
) NULL,
    metro_station varchar
(
    255
) NULL,
    price numeric
(
    12,
    2
) NULL,
    currency varchar
(
    10
) NULL,
    rooms int4 NULL,
    area_total float4 NULL,
    floor int4 NULL,
    floors_total int4 NULL,
    furniture bool DEFAULT false NOT NULL,
    photos _varchar DEFAULT '{}':: character varying [] NOT NULL,
    is_active bool DEFAULT true NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    parsed_at timestamptz DEFAULT now
(
) NOT NULL,
    updated_at_provider timestamptz NULL,
    city varchar
(
    255
) NULL,
    category varchar
(
    10
) DEFAULT 'FLAT':: character varying NOT NULL,
    room_area float4 NULL,
    geo_lat float8 NULL,
    geo_lng float8 NULL,
    CONSTRAINT chk_listings_category CHECK
(
    (
(
    category
)::text = ANY
(
    (
    ARRAY
[
    'FLAT'
    :
    :
    character
    varying,
    'ROOM'
    :
    :
    character
    varying]
)::text[]))),
    CONSTRAINT chk_listings_source CHECK
(
    (
(
    source
)::text = ANY
(
    (
    ARRAY
[
    'KUFAR'
    :
    :
    character
    varying,
    'REALT'
    :
    :
    character
    varying,
    'ONLINER'
    :
    :
    character
    varying]
)::text[]))),
    CONSTRAINT listings_pkey PRIMARY KEY
(
    id
),
    CONSTRAINT uk_listings_source_source_id UNIQUE
(
    source,
    source_id
)
    );
CREATE INDEX IF NOT EXISTS idx_listings_category_city ON public.listings USING btree (category, city) WHERE (is_active = true);
CREATE INDEX IF NOT EXISTS idx_listings_city ON public.listings USING btree (city);
CREATE INDEX IF NOT EXISTS idx_listings_parsed_at ON public.listings USING btree (parsed_at);
CREATE INDEX IF NOT EXISTS idx_listings_source ON public.listings USING btree (source);
CREATE INDEX IF NOT EXISTS idx_listings_source_parsed_at ON public.listings USING btree (source, parsed_at);

-- public.favorites definition

-- Drop table

-- DROP TABLE public.favorites;

CREATE TABLE IF NOT EXISTS public.favorites
(
    user_id
    int8
    NOT
    NULL,
    listing_id
    int8
    NOT
    NULL,
    listing_status_id
    int8
    NOT
    NULL,
    viewing_date
    timestamptz
    NULL,
    created_at
    timestamptz
    DEFAULT
    now
(
) NOT NULL,
    updated_at timestamptz DEFAULT now
(
) NOT NULL,
    CONSTRAINT favorites_pkey PRIMARY KEY
(
    user_id,
    listing_id
)
    );
CREATE INDEX IF NOT EXISTS idx_favorites_listing_id ON public.favorites USING btree (listing_id);
CREATE INDEX IF NOT EXISTS idx_favorites_status_id ON public.favorites USING btree (listing_status_id);
CREATE INDEX IF NOT EXISTS idx_favorites_user_id ON public.favorites USING btree (user_id);
CREATE INDEX IF NOT EXISTS idx_favorites_viewing_date_not_null ON public.favorites USING btree (viewing_date) WHERE (viewing_date IS NOT NULL);


-- public.favorites foreign keys
-- user_id is a user of identity-ms (another database), so it isn't referenced

DO
$$
BEGIN
    IF
NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_favorites_listing' AND conrelid = 'public.favorites'::regclass) THEN
ALTER TABLE public.favorites
    ADD CONSTRAINT fk_favorites_listing FOREIGN KEY (listing_id) REFERENCES public.listings (id) ON DELETE CASCADE;
END IF;
END $$;
DO
$$
BEGIN
    IF
NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_favorites_status' AND conrelid = 'public.favorites'::regclass) THEN
ALTER TABLE public.favorites
    ADD CONSTRAINT fk_favorites_status FOREIGN KEY (listing_status_id) REFERENCES public.listing_status (id);
END IF;
END $$;

-- public.currency_rates definition

-- Drop table

-- DROP TABLE public.currency_rates;

CREATE TABLE IF NOT EXISTS public.currency_rates
(
    id
    bigserial
    NOT
    NULL,
    rate_date
    date
    NOT
    NULL,
    currency
    varchar
(
    3
) NOT NULL,
    per_currency varchar
(
    3
) NOT NULL,
    rate numeric
(
    18,
    6
) NOT NULL,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT currency_rates_pkey PRIMARY KEY
(
    id
),
    CONSTRAINT uq_currency_rates_date_currency_per_currency UNIQUE
(
    rate_date,
    currency,
    per_currency
)
    );
CREATE INDEX IF NOT EXISTS idx_currency_rates_currency ON public.currency_rates USING btree (currency, per_currency);
CREATE INDEX IF NOT EXISTS idx_currency_rates_date ON public.currency_rates USING btree (rate_date);


-- public.hidden_listings definition

-- Drop table

-- DROP TABLE public.hidden_listings;

CREATE TABLE IF NOT EXISTS public.hidden_listings
(
    user_id
    int8
    NOT
    NULL,
    listing_id
    int8
    NOT
    NULL,
    created_at
    timestamptz
    DEFAULT
    now
(
) NOT NULL,
    CONSTRAINT hidden_listings_pkey PRIMARY KEY
(
    user_id,
    listing_id
)
    );
CREATE INDEX IF NOT EXISTS idx_hidden_listings_listing_id ON public.hidden_listings USING btree (listing_id);
CREATE INDEX IF NOT EXISTS idx_hidden_listings_user_id ON public.hidden_listings USING btree (user_id);


-- public.hidden_listings foreign keys
-- user_id is a user of identity-ms (another database), so it isn't referenced

DO
$$
BEGIN
    IF
NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_hidden_listings_listing' AND conrelid = 'public.hidden_listings'::regclass) THEN
ALTER TABLE public.hidden_listings
    ADD CONSTRAINT fk_hidden_listings_listing FOREIGN KEY (listing_id) REFERENCES public.listings (id) ON DELETE CASCADE;
END IF;
END $$;


-- public.v_calendar_events source

CREATE
OR REPLACE VIEW public.v_calendar_events
AS
SELECT f.user_id,
       f.listing_id,
       l.title,
       l.street,
       l.metro_station,
       l.price,
       l.currency,
       CASE
           WHEN array_length(l.photos, 1) > 0 THEN l.photos[1]
           ELSE NULL::character varying
END
AS cover_photo_url,
    f.viewing_date,
    ls.name AS status,
    f.created_at,
    f.updated_at
   FROM favorites f
     JOIN listings l ON l.id = f.listing_id
     JOIN listing_status ls ON ls.id = f.listing_status_id
  WHERE f.viewing_date IS NOT NULL AND ls.shows_in_calendar = true;

