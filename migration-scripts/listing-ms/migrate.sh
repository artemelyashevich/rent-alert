#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"

usage() {
  cat <<'USAGE'
Usage: migrate.sh [--dry-run]

Copies the listing domain from the monolith database (MONOLITH_DB_URL) into the
listing-ms database (TARGET_DB_URL): listing statuses, listings, favorites,
hidden listings and currency rates. Identifiers are preserved.
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

migrate_listing_status() {
  stage_copy listing_status_stg \
    "SELECT id, name, color, description, created_at, updated_at, shows_in_calendar FROM public.listing_status" \
    "id bigint, name varchar(50), color varchar(7), description varchar(255), created_at timestamptz, updated_at timestamptz, shows_in_calendar boolean"
  dst_exec "INSERT INTO listing_status (id, name, color, description, created_at, updated_at, shows_in_calendar) SELECT id, name, color, description, created_at, updated_at, shows_in_calendar FROM listing_status_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence listing_status id
  drop_staging listing_status_stg
  compare_counts listing_status \
    "SELECT count(*) FROM public.listing_status" \
    "SELECT count(*) FROM listing_status"
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

migrate_favorites() {
  stage_copy favorites_stg \
    "SELECT user_id, listing_id, listing_status_id, viewing_date, created_at, updated_at FROM public.favorites" \
    "user_id bigint, listing_id bigint, listing_status_id bigint, viewing_date timestamptz, created_at timestamptz, updated_at timestamptz"
  dst_exec "INSERT INTO favorites (user_id, listing_id, listing_status_id, viewing_date, created_at, updated_at) SELECT user_id, listing_id, listing_status_id, viewing_date, created_at, updated_at FROM favorites_stg ON CONFLICT (user_id, listing_id) DO NOTHING"
  drop_staging favorites_stg
  compare_counts favorites \
    "SELECT count(*) FROM public.favorites" \
    "SELECT count(*) FROM favorites"
}

migrate_hidden_listings() {
  stage_copy hidden_listings_stg \
    "SELECT user_id, listing_id, created_at FROM public.hidden_listings" \
    "user_id bigint, listing_id bigint, created_at timestamptz"
  dst_exec "INSERT INTO hidden_listings (user_id, listing_id, created_at) SELECT user_id, listing_id, created_at FROM hidden_listings_stg ON CONFLICT (user_id, listing_id) DO NOTHING"
  drop_staging hidden_listings_stg
  compare_counts hidden_listings \
    "SELECT count(*) FROM public.hidden_listings" \
    "SELECT count(*) FROM hidden_listings"
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

main() {
  parse_args "$@"
  require_env MONOLITH_DB_URL TARGET_DB_URL
  run_step listing_status migrate_listing_status
  run_step listings migrate_listings
  run_step favorites migrate_favorites
  run_step hidden_listings migrate_hidden_listings
  run_step currency_rates migrate_currency_rates
  log "migration finished"
}

main "$@"
