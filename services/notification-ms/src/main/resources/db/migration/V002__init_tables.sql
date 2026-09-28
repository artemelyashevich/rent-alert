-- public.notifications definition

-- Drop table

-- DROP TABLE public.notifications;

CREATE TABLE IF NOT EXISTS public.notifications (
                                      id bigserial NOT NULL,
                                      engine varchar(50) NOT NULL,
                                      receiver varchar(255) NOT NULL,
                                      "content" text NULL,
                                      status varchar(50) DEFAULT 'PENDING'::character varying NOT NULL,
                                      created_at timestamptz DEFAULT now() NOT NULL,
                                      updated_at timestamptz DEFAULT now() NOT NULL,
                                      attempt int4 DEFAULT 1 NOT NULL,
                                      error_message text NULL,
                                      next_retry_at timestamptz NULL,
                                      deduplication_key varchar(255) NULL,
                                      template_params_json text NULL,
                                      retry_type text NULL,
                                      listing_id int8 NULL,
                                      user_id int8 NULL,
                                      CONSTRAINT notifications_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_notifications_listing_id ON public.notifications USING btree (listing_id) WHERE (listing_id IS NOT NULL);
CREATE INDEX IF NOT EXISTS idx_notifications_receiver ON public.notifications USING btree (receiver);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON public.notifications USING btree (status);
CREATE INDEX IF NOT EXISTS idx_notifications_status_created_at ON public.notifications USING btree (status, created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON public.notifications USING btree (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_notifications_dedup_success ON public.notifications USING btree (deduplication_key, engine) WHERE (((status)::text = 'SUCCESS'::text) AND (deduplication_key IS NOT NULL));

-- public.notification_templates definition

-- Drop table

-- DROP TABLE public.notification_templates;

CREATE TABLE IF NOT EXISTS public.notification_templates (
                                               id bigserial NOT NULL,
                                               notification_type varchar(50) NOT NULL,
                                               engine varchar(50) NOT NULL,
                                               template_text text NOT NULL,
                                               created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                               updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                               template_name text NULL,
                                               CONSTRAINT notification_templates_pkey PRIMARY KEY (id),
                                               CONSTRAINT uk_notification_templates_type_engine UNIQUE (notification_type, engine)
);
-- The unique constraint already indexes (notification_type, engine); drop copies left by an older schema
ALTER TABLE public.notification_templates DROP CONSTRAINT IF EXISTS uq_notification_type_engine;
DROP INDEX IF EXISTS public.idx_templates_type_engine;

-- public.notification_broadcasts definition

-- Drop table

-- DROP TABLE public.notification_broadcasts;

CREATE TABLE IF NOT EXISTS public.notification_broadcasts (
                                                id bigserial NOT NULL,
                                                created_by int8 NULL,
                                                message text NOT NULL,
                                                total_recipients int4 DEFAULT 0 NOT NULL,
                                                sent_count int4 DEFAULT 0 NOT NULL,
                                                failed_count int4 DEFAULT 0 NOT NULL,
                                                skipped_count int4 DEFAULT 0 NOT NULL,
                                                created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                                updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                                pinned bool DEFAULT false NOT NULL,
                                                CONSTRAINT notification_broadcasts_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_notification_broadcasts_created_at ON public.notification_broadcasts USING btree (created_at DESC);


-- public.notification_broadcasts: created_by is a user of identity-ms (another database), so no foreign key


-- public.notification_broadcast_recipients definition

-- Drop table

-- DROP TABLE public.notification_broadcast_recipients;

CREATE TABLE IF NOT EXISTS public.notification_broadcast_recipients (
                                                          id bigserial NOT NULL,
                                                          broadcast_id int8 NOT NULL,
                                                          user_id int8 NOT NULL,
                                                          status varchar(50) NOT NULL,
                                                          error_message text NULL,
                                                          created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                                          updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                                          CONSTRAINT notification_broadcast_recipients_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_broadcast_recipients_broadcast_id ON public.notification_broadcast_recipients USING btree (broadcast_id);
CREATE INDEX IF NOT EXISTS idx_broadcast_recipients_status ON public.notification_broadcast_recipients USING btree (status);
CREATE INDEX IF NOT EXISTS idx_broadcast_recipients_user_id ON public.notification_broadcast_recipients USING btree (user_id);


-- public.notification_broadcast_recipients foreign keys
-- user_id is a user of identity-ms (another database), so it isn't referenced

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_broadcast_recipients_broadcast' AND conrelid = 'public.notification_broadcast_recipients'::regclass) THEN
        ALTER TABLE public.notification_broadcast_recipients ADD CONSTRAINT fk_broadcast_recipients_broadcast FOREIGN KEY (broadcast_id) REFERENCES public.notification_broadcasts(id) ON DELETE CASCADE;
    END IF;
END $$;


-- public.reminders definition

-- Drop table

-- DROP TABLE public.reminders;

CREATE TABLE IF NOT EXISTS public.reminders (
                                  id bigserial NOT NULL,
                                  user_id int8 NOT NULL,
                                  remind_at timestamptz NOT NULL,
                                  status varchar(50) DEFAULT 'NEW'::character varying NOT NULL,
                                  notification_template_id int8 NULL,
                                  interval_type varchar(50) DEFAULT 'BEFORE_1_DAY'::character varying NOT NULL,
                                  sent_at timestamptz NULL,
                                  attempts int4 DEFAULT 0 NOT NULL,
                                  "error" text NULL,
                                  created_at timestamptz DEFAULT now() NOT NULL,
                                  updated_at timestamptz DEFAULT now() NOT NULL,
                                  listing_id int8 NOT NULL,
                                  roommate_viewing_id int8 NULL,
                                  CONSTRAINT reminders_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_reminders_remind_at ON public.reminders USING btree (remind_at);
CREATE INDEX IF NOT EXISTS idx_reminders_roommate_viewing ON public.reminders USING btree (roommate_viewing_id) WHERE (roommate_viewing_id IS NOT NULL);
CREATE INDEX IF NOT EXISTS idx_reminders_status ON public.reminders USING btree (status);
CREATE INDEX IF NOT EXISTS idx_reminders_status_remind_at ON public.reminders USING btree (status, remind_at);
CREATE INDEX IF NOT EXISTS idx_reminders_user_id ON public.reminders USING btree (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_reminders_active_per_favorite ON public.reminders USING btree (user_id, listing_id, interval_type) WHERE ((status)::text = ANY ((ARRAY['NEW'::character varying, 'PROCESSING'::character varying])::text[]));


-- public.reminders foreign keys
-- user_id, listing_id and roommate_viewing_id belong to other services' databases, so only the template is referenced

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_reminders_notification_template' AND conrelid = 'public.reminders'::regclass) THEN
        ALTER TABLE public.reminders ADD CONSTRAINT fk_reminders_notification_template FOREIGN KEY (notification_template_id) REFERENCES public.notification_templates(id) ON DELETE SET NULL;
    END IF;
END $$;


-- public.user_notification_settings definition

-- Drop table

-- DROP TABLE public.user_notification_settings;

CREATE TABLE IF NOT EXISTS public.user_notification_settings (
                                                   id bigserial NOT NULL,
                                                   user_id int8 NOT NULL,
                                                   engine varchar(50) NOT NULL,
                                                   is_enabled bool DEFAULT true NOT NULL,
                                                   is_default bool DEFAULT false NOT NULL,
                                                   created_at timestamp DEFAULT now() NOT NULL,
                                                   updated_at timestamp DEFAULT now() NOT NULL,
                                                   CONSTRAINT uk_user_engine UNIQUE (user_id, engine),
                                                   CONSTRAINT user_notification_settings_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_user_notification_settings_user_id ON public.user_notification_settings USING btree (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_notification_settings_single_default ON public.user_notification_settings USING btree (user_id) WHERE (is_default = true);


-- public.user_notification_settings: user_id is a user of identity-ms (another database), so no foreign key
