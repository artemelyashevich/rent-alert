#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"

usage() {
  cat <<'USAGE'
Usage: migrate.sh [--dry-run]

Copies the processing domain from the monolith database (MONOLITH_DB_URL) into
the processing-ms database (TARGET_DB_URL): parser cities, scheduler settings,
schedule tasks, scheduled task records, currency rates and parsed listings.
Identifiers are preserved. Reference rows already seeded by processing-ms
migrations (V004) are matched by their natural key and are not overwritten.
Steps are idempotent: re-running does not duplicate rows.

Environment:
  MONOLITH_DB_URL   libpq URI of the monolith database
  TARGET_DB_URL     libpq URI of this service's database

Options:
  --dry-run         print target writes instead of executing them; source and target counts are still read
  -h, --help        show this help
USAGE
}

parse_args() {
  while (( $# > 0 )); do
    case "$1" in
      --dry-run) DRY_RUN=1 ;;
      -h|--help) usage; exit 0 ;;
      *) usage >&2; die "unknown argument: $1" ;;
    esac
    shift
  done
}

migrate_cities() {
  stage_copy cities_stg \
    "SELECT id, provider, name, city_code, created_at, updated_at, is_active FROM public.cities" \
    "id bigint, provider varchar(20), name varchar(255), city_code varchar(255), created_at timestamp, updated_at timestamp, is_active boolean"
  dst_exec "INSERT INTO cities (id, provider, name, city_code, created_at, updated_at, is_active) SELECT s.id, s.provider, s.name, s.city_code, s.created_at, s.updated_at, s.is_active FROM cities_stg s WHERE NOT EXISTS (SELECT 1 FROM cities c WHERE c.provider = s.provider AND c.city_code = s.city_code) ON CONFLICT (id) DO NOTHING"
  reset_sequence cities id
  dst_exec "INSERT INTO cities (provider, name, city_code, created_at, updated_at, is_active) SELECT s.provider, s.name, s.city_code, s.created_at, s.updated_at, s.is_active FROM cities_stg s WHERE NOT EXISTS (SELECT 1 FROM cities c WHERE c.provider = s.provider AND c.city_code = s.city_code) ON CONFLICT (provider, city_code) DO NOTHING"
  reset_sequence cities id
  drop_staging cities_stg
  compare_counts cities \
    "SELECT count(*) FROM public.cities" \
    "SELECT count(*) FROM cities"
}

migrate_scheduler_settings() {
  stage_copy scheduler_settings_stg \
    "SELECT id, provider_name, created_at, updated_at, scheduler_enabled, notifications_enabled, fixed_rate_ms, page_size, price_from, price_to, price_currency, region_id, sort_by, sort_order, category_id, room_category_id FROM public.scheduler_settings" \
    "id bigint, provider_name varchar(100), created_at timestamptz, updated_at timestamptz, scheduler_enabled boolean, notifications_enabled boolean, fixed_rate_ms bigint, page_size integer, price_from integer, price_to integer, price_currency varchar(10), region_id varchar(255), sort_by varchar(50), sort_order varchar(10), category_id integer, room_category_id integer"
  dst_exec "INSERT INTO scheduler_settings (id, provider_name, created_at, updated_at, scheduler_enabled, notifications_enabled, fixed_rate_ms, page_size, price_from, price_to, price_currency, region_id, sort_by, sort_order, category_id, room_category_id) SELECT s.id, s.provider_name, s.created_at, s.updated_at, s.scheduler_enabled, s.notifications_enabled, s.fixed_rate_ms, s.page_size, s.price_from, s.price_to, s.price_currency, s.region_id, s.sort_by, s.sort_order, s.category_id, s.room_category_id FROM scheduler_settings_stg s WHERE NOT EXISTS (SELECT 1 FROM scheduler_settings t WHERE t.provider_name = s.provider_name) ON CONFLICT (id) DO NOTHING"
  reset_sequence scheduler_settings id
  dst_exec "INSERT INTO scheduler_settings (provider_name, created_at, updated_at, scheduler_enabled, notifications_enabled, fixed_rate_ms, page_size, price_from, price_to, price_currency, region_id, sort_by, sort_order, category_id, room_category_id) SELECT s.provider_name, s.created_at, s.updated_at, s.scheduler_enabled, s.notifications_enabled, s.fixed_rate_ms, s.page_size, s.price_from, s.price_to, s.price_currency, s.region_id, s.sort_by, s.sort_order, s.category_id, s.room_category_id FROM scheduler_settings_stg s WHERE NOT EXISTS (SELECT 1 FROM scheduler_settings t WHERE t.provider_name = s.provider_name) ON CONFLICT (provider_name) DO NOTHING"
  reset_sequence scheduler_settings id
  drop_staging scheduler_settings_stg
  compare_counts scheduler_settings \
    "SELECT count(*) FROM public.scheduler_settings" \
    "SELECT count(*) FROM scheduler_settings"
}

migrate_schedule_tasks() {
  stage_copy schedule_tasks_stg \
    "SELECT id, last_processed_id, attempt, provider, scrap, last_sync_at, created_at, updated_at FROM public.schedule_tasks" \
    "id bigint, last_processed_id varchar(255), attempt bigint, provider varchar(50), scrap boolean, last_sync_at timestamptz, created_at timestamptz, updated_at timestamptz"
  dst_exec "INSERT INTO schedule_tasks (id, last_processed_id, attempt, provider, scrap, last_sync_at, created_at, updated_at) SELECT id, last_processed_id, attempt, provider, scrap, last_sync_at, created_at, updated_at FROM schedule_tasks_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence schedule_tasks id
  drop_staging schedule_tasks_stg
  compare_counts schedule_tasks \
    "SELECT count(*) FROM public.schedule_tasks" \
    "SELECT count(*) FROM schedule_tasks"
}

migrate_scheduled_task_records() {
  stage_copy scheduled_task_records_stg \
    "SELECT id, provider, task_key, status, error_message, processed_at, created_at, updated_at FROM public.scheduled_task_records" \
    "id bigint, provider varchar(64), task_key varchar(255), status varchar(16), error_message text, processed_at timestamptz, created_at timestamptz, updated_at timestamptz"
  dst_exec "INSERT INTO scheduled_task_records (id, provider, task_key, status, error_message, processed_at, created_at, updated_at) SELECT id, provider, task_key, status, error_message, processed_at, created_at, updated_at FROM scheduled_task_records_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence scheduled_task_records id
  drop_staging scheduled_task_records_stg
  compare_counts scheduled_task_records \
    "SELECT count(*) FROM public.scheduled_task_records" \
    "SELECT count(*) FROM scheduled_task_records"
}

migrate_currency_rates() {
  stage_copy currency_rates_stg \
    "SELECT id, rate_date, currency, per_currency, rate, created_at, updated_at FROM public.currency_rates" \
    "id bigint, rate_date date, currency varchar(3), per_currency varchar(3), rate numeric(18,6), created_at timestamp, updated_at timestamp"
  dst_exec "INSERT INTO currency_rates (id, rate_date, currency, per_currency, rate, created_at, updated_at) SELECT id, rate_date, currency, per_currency, rate, created_at, updated_at FROM currency_rates_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence currency_rates id
  drop_staging currency_rates_stg
  compare_counts currency_rates \
    "SELECT count(*) FROM public.currency_rates" \
    "SELECT count(*) FROM currency_rates"
}

migrate_listings() {
  stage_copy listings_stg \
    "SELECT id, source, source_id, source_url, title, description, street, metro_station, city, price, currency, rooms, area_total, room_area, geo_lat, geo_lng, floor, floors_total, furniture, photos, is_active, created_at, updated_at, parsed_at, updated_at_provider, category FROM public.listings" \
    "id bigint, source varchar(20), source_id varchar(255), source_url varchar(2048), title varchar(1000), description text, street varchar(255), metro_station varchar(255), city varchar(255), price numeric(12,2), currency varchar(10), rooms integer, area_total real, room_area real, geo_lat double precision, geo_lng double precision, floor integer, floors_total integer, furniture boolean, photos varchar[], is_active boolean, created_at timestamptz, updated_at timestamptz, parsed_at timestamptz, updated_at_provider timestamptz, category varchar(10)"
  dst_exec "INSERT INTO listings (id, source, source_id, source_url, title, description, street, metro_station, city, price, currency, rooms, area_total, room_area, geo_lat, geo_lng, floor, floors_total, furniture, photos, is_active, created_at, updated_at, parsed_at, updated_at_provider, category) SELECT id, source, source_id, source_url, title, description, street, metro_station, city, price, currency, rooms, area_total, room_area, geo_lat, geo_lng, floor, floors_total, furniture, photos, is_active, created_at, updated_at, parsed_at, updated_at_provider, category FROM listings_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence listings id
  drop_staging listings_stg
  compare_counts listings \
    "SELECT count(*) FROM public.listings" \
    "SELECT count(*) FROM listings"
}

main() {
  parse_args "$@"
  require_env MONOLITH_DB_URL TARGET_DB_URL
  run_step cities migrate_cities
  run_step scheduler_settings migrate_scheduler_settings
  run_step schedule_tasks migrate_schedule_tasks
  run_step scheduled_task_records migrate_scheduled_task_records
  run_step currency_rates migrate_currency_rates
  run_step listings migrate_listings
  log "migration finished"
}

main "$@"
