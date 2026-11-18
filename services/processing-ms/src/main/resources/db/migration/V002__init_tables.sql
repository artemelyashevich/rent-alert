
-- public.cities definition

CREATE TABLE IF NOT EXISTS public.cities
(
    id BIGSERIAL NOT NULL,
    provider VARCHAR(20) NOT NULL,
    "name" VARCHAR(255) NOT NULL,
    city_code VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    updated_at TIMESTAMP DEFAULT now() NOT NULL,
    is_active BOOL DEFAULT true NOT NULL,
    CONSTRAINT chk_cities_provider CHECK (
                                             provider IN ('KUFAR', 'REALT', 'ONLINER')
    ),
    CONSTRAINT cities_pkey PRIMARY KEY (id),
    CONSTRAINT uk_cities_provider_city_code UNIQUE (provider, city_code)
    );

CREATE INDEX IF NOT EXISTS idx_cities_name ON public.cities USING btree (name);
CREATE INDEX IF NOT EXISTS idx_cities_provider ON public.cities USING btree (provider);
CREATE INDEX IF NOT EXISTS idx_cities_provider_active ON public.cities USING btree (provider, is_active);


-- public.currency_rates definition

CREATE TABLE IF NOT EXISTS public.currency_rates
(
    id BIGSERIAL NOT NULL,
    rate_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    per_currency VARCHAR(3) NOT NULL,
    rate NUMERIC(18, 6) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT currency_rates_pkey PRIMARY KEY (id),
    CONSTRAINT uq_currency_rates_date_currency_per_currency
    UNIQUE (rate_date, currency, per_currency)
    );

CREATE INDEX IF NOT EXISTS idx_currency_rates_currency
    ON public.currency_rates USING btree (currency, per_currency);
CREATE INDEX IF NOT EXISTS idx_currency_rates_date
    ON public.currency_rates USING btree (rate_date);


-- public.listings definition

CREATE TABLE IF NOT EXISTS public.listings
(
    id BIGSERIAL NOT NULL,
    "source" VARCHAR(20) NOT NULL,
    source_id VARCHAR(255) NOT NULL,
    source_url VARCHAR(2048) NULL,
    title VARCHAR(1000) NULL,
    description TEXT NULL,
    street VARCHAR(255) NULL,
    metro_station VARCHAR(255) NULL,
    price NUMERIC(12, 2) NULL,
    currency VARCHAR(10) NULL,
    rooms INT4 NULL,
    area_total FLOAT4 NULL,
    floor INT4 NULL,
    floors_total INT4 NULL,
    furniture BOOL DEFAULT false NOT NULL,
    photos _varchar DEFAULT '{}'::character varying[] NOT NULL,
    is_active BOOL DEFAULT true NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    parsed_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at_provider TIMESTAMPTZ NULL,
    city VARCHAR(255) NULL,
    category VARCHAR(10) DEFAULT 'FLAT' NOT NULL,
    room_area FLOAT4 NULL,
    geo_lat FLOAT8 NULL,
    geo_lng FLOAT8 NULL,
    CONSTRAINT chk_listings_category CHECK (
                                               category IN ('FLAT', 'ROOM')
    ),
    CONSTRAINT chk_listings_source CHECK (
                                             "source" IN ('KUFAR', 'REALT', 'ONLINER')
    ),
    CONSTRAINT listings_pkey PRIMARY KEY (id),
    CONSTRAINT uk_listings_source_source_id UNIQUE ("source", source_id)
    );

CREATE INDEX IF NOT EXISTS idx_listings_category_city
    ON public.listings USING btree (category, city) WHERE (is_active = true);
CREATE INDEX IF NOT EXISTS idx_listings_city ON public.listings USING btree (city);
CREATE INDEX IF NOT EXISTS idx_listings_parsed_at ON public.listings USING btree (parsed_at);
CREATE INDEX IF NOT EXISTS idx_listings_source ON public.listings USING btree ("source");
CREATE INDEX IF NOT EXISTS idx_listings_source_parsed_at
    ON public.listings USING btree ("source", parsed_at);


-- public.schedule_tasks definition

CREATE TABLE IF NOT EXISTS public.schedule_tasks
(
    id BIGSERIAL NOT NULL,
    last_processed_id VARCHAR(255) NULL,
    attempt INT8 DEFAULT 1 NOT NULL,
    provider VARCHAR(50) NOT NULL,
    scrap BOOL DEFAULT false NOT NULL,
    last_sync_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    CONSTRAINT schedule_tasks_pkey PRIMARY KEY (id)
    );

CREATE INDEX IF NOT EXISTS idx_schedule_tasks_provider
    ON public.schedule_tasks USING btree (provider);
CREATE INDEX IF NOT EXISTS idx_schedule_tasks_scrap
    ON public.schedule_tasks USING btree (scrap);


-- public.scheduled_task_records definition

CREATE TABLE IF NOT EXISTS public.scheduled_task_records
(
    id BIGSERIAL NOT NULL,
    provider VARCHAR(64) NOT NULL,
    task_key VARCHAR(255) NOT NULL,
    status VARCHAR(16) DEFAULT 'PENDING' NOT NULL,
    error_message TEXT NULL,
    processed_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT scheduled_task_records_pkey PRIMARY KEY (id),
    CONSTRAINT uq_task_records_provider_key UNIQUE (provider, task_key)
    );

CREATE INDEX IF NOT EXISTS idx_task_records_created_at
    ON public.scheduled_task_records USING btree (created_at);
CREATE INDEX IF NOT EXISTS idx_task_records_processed_at
    ON public.scheduled_task_records USING btree (processed_at);
CREATE INDEX IF NOT EXISTS idx_task_records_success_provider_processed_at
    ON public.scheduled_task_records USING btree (provider, processed_at)
    WHERE ((status)::text = 'SUCCESS'::text);


-- public.scheduler_settings definition

CREATE TABLE IF NOT EXISTS public.scheduler_settings
(
    id BIGSERIAL NOT NULL,
    provider_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    scheduler_enabled BOOL DEFAULT false NOT NULL,
    notifications_enabled BOOL DEFAULT false NOT NULL,
    fixed_rate_ms INT8 DEFAULT 60000 NOT NULL,
    page_size INT4 DEFAULT 50 NOT NULL,
    price_from INT4 NULL,
    price_to INT4 NULL,
    price_currency VARCHAR(10) DEFAULT 'BYN' NULL,
    region_id VARCHAR(255) DEFAULT '0' NOT NULL,
    sort_by VARCHAR(50) DEFAULT 'lst.d' NOT NULL,
    sort_order VARCHAR(10) DEFAULT 'DESC' NOT NULL,
    category_id INT4 DEFAULT 2 NOT NULL,
    room_category_id INT4 NULL,
    CONSTRAINT chk_price_currency CHECK (
                                            price_currency IN ('USD', 'BYN')
    ),
    CONSTRAINT chk_sort_order CHECK (
                                        sort_order IN ('ASC', 'DESC')
    ),
    CONSTRAINT scheduler_settings_pkey PRIMARY KEY (id),
    CONSTRAINT scheduler_settings_provider_name_key UNIQUE (provider_name)
    );

CREATE INDEX IF NOT EXISTS idx_scheduler_settings_region_category
    ON public.scheduler_settings USING btree (region_id, category_id);
