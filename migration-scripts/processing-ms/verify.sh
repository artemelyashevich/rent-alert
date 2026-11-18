#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"

usage() {
  cat <<'USAGE'
Usage: verify.sh

Compares the processing domain between the monolith database (MONOLITH_DB_URL)
and the processing-ms database (TARGET_DB_URL): row counts, configuration values
of parser cities and scheduler settings (by natural key), schedule task cursors,
scheduled task record statuses, listings by source, currency rates by pair.
Read-only. Exits non-zero on any MISMATCH.

Environment:
  MONOLITH_DB_URL   libpq URI of the monolith database
  TARGET_DB_URL     libpq URI of this service's database

Options:
  -h, --help        show this help
USAGE
}

parse_args() {
  while (( $# > 0 )); do
    case "$1" in
      -h|--help) usage; exit 0 ;;
      *) usage >&2; die "unknown argument: $1" ;;
    esac
    shift
  done
}

compare_output() {
  local label=$1 src_sql=$2 dst_sql=$3 src dst
  src=$(src_psql -At -c "$src_sql" | LC_ALL=C sort)
  dst=$(dst_psql -At -c "$dst_sql" | LC_ALL=C sort)
  if [[ $src == "$dst" ]]; then
    log "[$label] OK"
  else
    log "[$label] MISMATCH"
    diff <(printf '%s\n' "$src") <(printf '%s\n' "$dst") >&2 || true
    return 1
  fi
}

verify_cities() {
  compare_counts cities \
    "SELECT count(*) FROM public.cities" \
    "SELECT count(*) FROM cities"
  compare_output cities_by_natural_key \
    "SELECT provider, city_code, name, is_active FROM public.cities" \
    "SELECT provider, city_code, name, is_active FROM cities"
}

verify_scheduler_settings() {
  compare_counts scheduler_settings \
    "SELECT count(*) FROM public.scheduler_settings" \
    "SELECT count(*) FROM scheduler_settings"
  compare_output scheduler_settings_values \
    "SELECT provider_name, scheduler_enabled, notifications_enabled, fixed_rate_ms, page_size, price_from, price_to, price_currency, region_id, sort_by, sort_order, category_id, room_category_id FROM public.scheduler_settings" \
    "SELECT provider_name, scheduler_enabled, notifications_enabled, fixed_rate_ms, page_size, price_from, price_to, price_currency, region_id, sort_by, sort_order, category_id, room_category_id FROM scheduler_settings"
}

verify_schedule_tasks() {
  compare_counts schedule_tasks \
    "SELECT count(*) FROM public.schedule_tasks" \
    "SELECT count(*) FROM schedule_tasks"
  compare_output schedule_tasks_cursors \
    "SELECT id, provider, scrap, attempt, last_processed_id FROM public.schedule_tasks" \
    "SELECT id, provider, scrap, attempt, last_processed_id FROM schedule_tasks"
}

verify_scheduled_task_records() {
  compare_counts scheduled_task_records \
    "SELECT count(*) FROM public.scheduled_task_records" \
    "SELECT count(*) FROM scheduled_task_records"
  compare_output scheduled_task_records_by_status \
    "SELECT provider, status, count(*) FROM public.scheduled_task_records GROUP BY 1, 2" \
    "SELECT provider, status, count(*) FROM scheduled_task_records GROUP BY 1, 2"
}

verify_currency_rates() {
  compare_counts currency_rates \
    "SELECT count(*) FROM public.currency_rates" \
    "SELECT count(*) FROM currency_rates"
  compare_output currency_rates_by_pair \
    "SELECT currency, per_currency, count(*) FROM public.currency_rates GROUP BY 1, 2" \
    "SELECT currency, per_currency, count(*) FROM currency_rates GROUP BY 1, 2"
}

verify_listings() {
  compare_counts listings \
    "SELECT count(*) FROM public.listings" \
    "SELECT count(*) FROM listings"
  compare_output listings_by_source_category_active \
    "SELECT source, category, is_active, count(*) FROM public.listings GROUP BY 1, 2, 3" \
    "SELECT source, category, is_active, count(*) FROM listings GROUP BY 1, 2, 3"
}

main() {
  parse_args "$@"
  require_env MONOLITH_DB_URL TARGET_DB_URL
  run_step cities verify_cities
  run_step scheduler_settings verify_scheduler_settings
  run_step schedule_tasks verify_schedule_tasks
  run_step scheduled_task_records verify_scheduled_task_records
  run_step currency_rates verify_currency_rates
  run_step listings verify_listings
  log "verification finished"
}

main "$@"
