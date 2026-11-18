#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"

usage() {
  cat <<'USAGE'
Usage: verify.sh

Compares the listing domain between the monolith database (MONOLITH_DB_URL) and
the listing-ms database (TARGET_DB_URL): row counts and control aggregates
(listings by source, category and activity; favorites by status; currency rates
by pair). Read-only. Exits non-zero on any MISMATCH.

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

verify_listing_status() {
  compare_counts listing_status \
    "SELECT count(*) FROM public.listing_status" \
    "SELECT count(*) FROM listing_status"
  compare_output listing_status_rows \
    "SELECT id, name, color, description, shows_in_calendar FROM public.listing_status" \
    "SELECT id, name, color, description, shows_in_calendar FROM listing_status"
}

verify_listings() {
  compare_counts listings \
    "SELECT count(*) FROM public.listings" \
    "SELECT count(*) FROM listings"
  compare_output listings_by_source_category_active \
    "SELECT source, category, is_active, count(*) FROM public.listings GROUP BY 1, 2, 3" \
    "SELECT source, category, is_active, count(*) FROM listings GROUP BY 1, 2, 3"
}

verify_favorites() {
  compare_counts favorites \
    "SELECT count(*) FROM public.favorites" \
    "SELECT count(*) FROM favorites"
  compare_output favorites_by_status \
    "SELECT listing_status_id, count(*), count(viewing_date) FROM public.favorites GROUP BY 1" \
    "SELECT listing_status_id, count(*), count(viewing_date) FROM favorites GROUP BY 1"
}

verify_hidden_listings() {
  compare_counts hidden_listings \
    "SELECT count(*) FROM public.hidden_listings" \
    "SELECT count(*) FROM hidden_listings"
}

verify_currency_rates() {
  compare_counts currency_rates \
    "SELECT count(*) FROM public.currency_rates" \
    "SELECT count(*) FROM currency_rates"
  compare_output currency_rates_by_pair \
    "SELECT currency, per_currency, count(*) FROM public.currency_rates GROUP BY 1, 2" \
    "SELECT currency, per_currency, count(*) FROM currency_rates GROUP BY 1, 2"
}

main() {
  parse_args "$@"
  require_env MONOLITH_DB_URL TARGET_DB_URL
  run_step listing_status verify_listing_status
  run_step listings verify_listings
  run_step favorites verify_favorites
  run_step hidden_listings verify_hidden_listings
  run_step currency_rates verify_currency_rates
  log "verification finished"
}

main "$@"
