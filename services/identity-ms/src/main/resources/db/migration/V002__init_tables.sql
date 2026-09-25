DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'bot_status_enum' AND typnamespace = 'public'::regnamespace) THEN
CREATE TYPE public.bot_status_enum AS ENUM ('ACTIVE', 'PAUSED', 'STOPPED');
END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.user_profiles (
                                      id bigserial NOT NULL,
                                      created_at timestamptz DEFAULT now() NOT NULL,
                                      updated_at timestamptz DEFAULT now() NOT NULL,
                                      telegram_id varchar(255) NULL,
                                      telegram_id_hash varchar(64) NULL,
                                      username varchar(255) NULL,
                                      last_login timestamptz DEFAULT now() NOT NULL,
                                      photo_url text NULL,
                                      CONSTRAINT user_profiles_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_profiles_username ON public.user_profiles USING btree (username);

CREATE TABLE IF NOT EXISTS public.user_filters (
                                     id bigserial NOT NULL,
                                     user_profile_id int8 NULL,
                                     price_from int4 NULL,
                                     price_to int4 NULL,
                                     rooms_from int4 NULL,
                                     rooms_to int4 NULL,
                                     area_from float8 NULL,
                                     area_to float8 NULL,
                                     is_active bool DEFAULT false NOT NULL,
                                     created_at timestamptz NOT NULL,
                                     updated_at timestamptz NOT NULL,
                                     currency varchar(5) DEFAULT 'USD'::character varying NOT NULL,
                                     city varchar(255) DEFAULT 'Минск'::character varying NOT NULL,
                                     owner_match_id int8 NULL,
                                     category varchar(10) NULL,
                                     CONSTRAINT chk_user_filters_category CHECK (((category IS NULL) OR ((category)::text = ANY ((ARRAY['FLAT'::character varying, 'ROOM'::character varying])::text[])))),
	CONSTRAINT chk_user_filters_single_owner CHECK (((user_profile_id IS NULL) <> (owner_match_id IS NULL))),
	CONSTRAINT pk_user_filters PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_user_filters_active_city ON public.user_filters USING btree (city) WHERE (is_active = true);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_filters_one_active_per_match ON public.user_filters USING btree (owner_match_id) WHERE ((is_active = true) AND (owner_match_id IS NOT NULL));
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_filters_one_active_per_profile ON public.user_filters USING btree (user_profile_id) WHERE (is_active = true);
CREATE INDEX IF NOT EXISTS idx_user_filters_owner_match ON public.user_filters USING btree (owner_match_id) WHERE (owner_match_id IS NOT NULL);
CREATE INDEX IF NOT EXISTS idx_user_filters_profile_id ON public.user_filters USING btree (user_profile_id);

CREATE OR REPLACE FUNCTION public.check_user_filters_limit()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    IF (SELECT COUNT(*) FROM user_filters WHERE user_profile_id = NEW.user_profile_id) >= 5 THEN
        RAISE EXCEPTION 'User profile % already has 5 filters (maximum allowed)', NEW.user_profile_id;
END IF;
RETURN NEW;
END;
$function$
;

DROP TRIGGER IF EXISTS trg_user_filters_limit ON public.user_filters;
create trigger trg_user_filters_limit before
    insert
    on
        public.user_filters for each row execute function check_user_filters_limit();


-- public.user_filters foreign keys

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_filters_user_profile' AND conrelid = 'public.user_filters'::regclass) THEN
        ALTER TABLE public.user_filters ADD CONSTRAINT fk_user_filters_user_profile FOREIGN KEY (user_profile_id) REFERENCES public.user_profiles(id) ON DELETE CASCADE;
    END IF;
END
$$;

-- public.user_settings definition

-- Drop table

-- DROP TABLE public.user_settings;

CREATE TABLE IF NOT EXISTS public.user_settings (
                                      id bigserial NOT NULL,
                                      user_profile_id int8 NOT NULL,
                                      email varchar(255) NULL,
                                      phone_number varchar(32) NULL,
                                      color_theme varchar(16) DEFAULT 'SYSTEM'::character varying NOT NULL,
                                      "language" varchar(8) DEFAULT 'RU'::character varying NOT NULL,
                                      refresh_token varchar(512) NULL,
                                      password_hash varchar(255) NULL,
                                      role_name varchar(50) DEFAULT 'USER'::character varying NOT NULL,
                                      bot_status public."bot_status_enum" DEFAULT 'ACTIVE'::bot_status_enum NOT NULL,
                                      quiet_from time NULL,
                                      quiet_to time NULL,
                                      is_email_active bool DEFAULT false NOT NULL,
                                      created_at timestamptz DEFAULT now() NOT NULL,
                                      updated_at timestamptz DEFAULT now() NOT NULL,
                                      CONSTRAINT user_settings_pkey PRIMARY KEY (id),
                                      CONSTRAINT user_settings_user_profile_id_key UNIQUE (user_profile_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_settings_email ON public.user_settings USING btree (email);
CREATE INDEX IF NOT EXISTS idx_user_settings_user_profile_id ON public.user_settings USING btree (user_profile_id);


-- public.user_settings foreign keys

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'user_settings_user_profile_id_fkey' AND conrelid = 'public.user_settings'::regclass) THEN
        ALTER TABLE public.user_settings ADD CONSTRAINT user_settings_user_profile_id_fkey FOREIGN KEY (user_profile_id) REFERENCES public.user_profiles(id) ON DELETE CASCADE;
    END IF;
END
$$;

-- public.feedbacks definition

-- Drop table

-- DROP TABLE public.feedbacks;

CREATE TABLE IF NOT EXISTS public.feedbacks (
                                  id bigserial NOT NULL,
                                  user_id int8 NULL,
                                  message varchar(2000) NULL,
                                  rating int4 DEFAULT 1 NULL,
                                  created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                  updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                  subject_type varchar(20) DEFAULT 'APP'::character varying NOT NULL,
                                  subject_id int8 NULL,
                                  reason_code varchar(20) NULL,
                                  CONSTRAINT chk_feedbacks_rating CHECK (((rating IS NULL) OR ((rating >= 1) AND (rating <= 5)))),
                                  CONSTRAINT feedbacks_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_feedbacks_created_at ON public.feedbacks USING btree (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_feedbacks_subject ON public.feedbacks USING btree (subject_type, subject_id) WHERE (subject_id IS NOT NULL);
CREATE INDEX IF NOT EXISTS idx_feedbacks_user_id ON public.feedbacks USING btree (user_id);


-- public.feedbacks foreign keys

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_feedbacks_user_id' AND conrelid = 'public.feedbacks'::regclass) THEN
        ALTER TABLE public.feedbacks ADD CONSTRAINT fk_feedbacks_user_id FOREIGN KEY (user_id) REFERENCES public.user_profiles(id) ON DELETE SET NULL;
    END IF;
END
$$;


-- public.detailed_user_profiles source

CREATE OR REPLACE VIEW public.detailed_user_profiles
AS SELECT u.id,
          u.username,
          u.photo_url,
          s.bot_status::text AS bot_status,
              s.role_name,
          u.last_login,
          s.quiet_from,
          s.quiet_to,
          s.email,
          u.created_at AS registered_at,
          u.updated_at,
          (( SELECT COALESCE(json_agg(row_to_json(t.*) ORDER BY t."createdAt" DESC), '[]'::json) AS "coalesce"
             FROM ( SELECT f.id,
                           f.price_from AS "priceFrom",
                           f.price_to AS "priceTo",
                           f.rooms_from AS "roomsFrom",
                           f.rooms_to AS "roomsTo",
                           f.area_from AS "areaFrom",
                           f.area_to AS "areaTo",
                           f.is_active AS active,
                           f.currency,
                           f.created_at AS "createdAt",
                           f.updated_at AS "updatedAt"
                    FROM user_filters f
                    WHERE f.user_profile_id = u.id AND f.is_active = true
                    ORDER BY f.created_at DESC
                        LIMIT 5) t))::text AS active_filters,
              (( SELECT COALESCE(json_agg(row_to_json(t.*) ORDER BY t."createdAt" DESC), '[]'::json) AS "coalesce"
                 FROM ( SELECT fb.id,
                               fb.user_id AS "userId",
                               u.username,
                               fb.message,
                               fb.rating,
                               fb.created_at AS "createdAt",
                               fb.updated_at AS "updatedAt"
                        FROM feedbacks fb
                        WHERE fb.user_id = u.id
                        ORDER BY fb.created_at DESC
                            LIMIT 5) t))::text AS recent_feedbacks
   FROM user_profiles u
            JOIN user_settings s ON s.user_profile_id = u.id;