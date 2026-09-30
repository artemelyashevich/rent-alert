-- public.cities definition

-- Drop table

-- DROP TABLE public.cities;

CREATE TABLE IF NOT EXISTS public.cities
(
    id
    bigserial
    NOT
    NULL,
    provider
    varchar
(
    20
) NOT NULL,
    "name" varchar
(
    255
) NOT NULL,
    city_code varchar
(
    255
) NOT NULL,
    created_at timestamp DEFAULT now
(
) NOT NULL,
    updated_at timestamp DEFAULT now
(
) NOT NULL,
    is_active bool DEFAULT true NOT NULL,
    CONSTRAINT chk_cities_provider CHECK
(
    (
(
    provider
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
    CONSTRAINT cities_pkey PRIMARY KEY
(
    id
),
    CONSTRAINT uk_cities_provider_city_code UNIQUE
(
    provider,
    city_code
)
    );
CREATE INDEX IF NOT EXISTS idx_cities_name ON public.cities USING btree (name);
CREATE INDEX IF NOT EXISTS idx_cities_provider ON public.cities USING btree (provider);
CREATE INDEX IF NOT EXISTS idx_cities_provider_active ON public.cities USING btree (provider, is_active);

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

-- public.schedule_tasks definition

-- Drop table

-- DROP TABLE public.schedule_tasks;

CREATE TABLE IF NOT EXISTS public.schedule_tasks
(
    id
    bigserial
    NOT
    NULL,
    last_processed_id
    varchar
(
    255
) NULL,
    attempt int8 DEFAULT 1 NOT NULL,
    provider varchar
(
    50
) NOT NULL,
    scrap bool DEFAULT false NOT NULL,
    last_sync_at timestamptz NULL,
    created_at timestamptz DEFAULT now
(
) NOT NULL,
    updated_at timestamptz DEFAULT now
(
) NOT NULL,
    CONSTRAINT schedule_tasks_pkey PRIMARY KEY
(
    id
)
    );
CREATE INDEX IF NOT EXISTS idx_schedule_tasks_provider ON public.schedule_tasks USING btree (provider);
CREATE INDEX IF NOT EXISTS idx_schedule_tasks_scrap ON public.schedule_tasks USING btree (scrap);

-- public.scheduled_task_records definition

-- Drop table

-- DROP TABLE public.scheduled_task_records;

CREATE TABLE IF NOT EXISTS public.scheduled_task_records
(
    id
    bigserial
    NOT
    NULL,
    provider
    varchar
(
    64
) NOT NULL,
    task_key varchar
(
    255
) NOT NULL,
    status varchar
(
    16
) DEFAULT 'PENDING':: character varying NOT NULL,
    error_message text NULL,
    processed_at timestamptz NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT scheduled_task_records_pkey PRIMARY KEY
(
    id
),
    CONSTRAINT uq_task_records_provider_key UNIQUE
(
    provider,
    task_key
)
    );
CREATE INDEX IF NOT EXISTS idx_task_records_created_at ON public.scheduled_task_records USING btree (created_at);
CREATE INDEX IF NOT EXISTS idx_task_records_processed_at ON public.scheduled_task_records USING btree (processed_at);
CREATE INDEX IF NOT EXISTS idx_task_records_success_provider_processed_at ON public.scheduled_task_records USING btree (provider, processed_at) WHERE ((status)::text = 'SUCCESS'::text);

-- public.scheduler_settings definition

-- Drop table

-- DROP TABLE public.scheduler_settings;

CREATE TABLE IF NOT EXISTS public.scheduler_settings
(
    id
    bigserial
    NOT
    NULL,
    provider_name
    varchar
(
    100
) NOT NULL,
    created_at timestamptz DEFAULT now
(
) NOT NULL,
    updated_at timestamptz DEFAULT now
(
) NOT NULL,
    scheduler_enabled bool DEFAULT false NOT NULL,
    notifications_enabled bool DEFAULT false NOT NULL,
    fixed_rate_ms int8 DEFAULT 60000 NOT NULL,
    page_size int4 DEFAULT 50 NOT NULL,
    price_from int4 NULL,
    price_to int4 NULL,
    price_currency varchar
(
    10
) DEFAULT 'BYN':: character varying NULL,
    region_id varchar
(
    255
) DEFAULT '0':: character varying NOT NULL,
    sort_by varchar
(
    50
) DEFAULT 'lst.d':: character varying NOT NULL,
    sort_order varchar
(
    10
) DEFAULT 'DESC':: character varying NOT NULL,
    category_id int4 DEFAULT 2 NOT NULL,
    room_category_id int4 NULL,
    CONSTRAINT chk_price_currency CHECK
(
    (
(
    price_currency
)::text = ANY
(
    (
    ARRAY
[
    'USD'
    :
    :
    character
    varying,
    'BYN'
    :
    :
    character
    varying]
)::text[]))),
    CONSTRAINT chk_sort_order CHECK
(
    (
(
    sort_order
)::text = ANY
(
    (
    ARRAY
[
    'ASC'
    :
    :
    character
    varying,
    'DESC'
    :
    :
    character
    varying]
)::text[]))),
    CONSTRAINT scheduler_settings_pkey PRIMARY KEY
(
    id
),
    CONSTRAINT scheduler_settings_provider_name_key UNIQUE
(
    provider_name
)
    );
CREATE INDEX IF NOT EXISTS idx_scheduler_settings_region_category ON public.scheduler_settings USING btree (region_id, category_id);