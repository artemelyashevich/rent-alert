#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"

SHARED_FAVORITE_STATUSES="'INTERESTED','WAITING_RESPONSE','VIEWING_SCHEDULED','RESERVED','VIEWED','AGREED'"

usage() {
  cat <<'USAGE'
Usage: verify.sh

Compares row counts and control aggregates between the monolith database
(MONOLITH_DB_URL) and the roommate-ms database (TARGET_DB_URL). Also checks that
no roommate_match_participants row points to a missing match. Read-only.
Exits non-zero on any MISMATCH or orphan.

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

expect_zero() {
  local label=$1 sql=$2 n
  n=$(dst_psql -At -c "$sql")
  if [[ $n == 0 ]]; then
    log "[$label] orphans=0 OK"
  else
    log "[$label] orphans=$n MISMATCH"
    return 1
  fi
}

verify_profiles() {
  compare_counts roommate_profiles \
    "SELECT count(*) FROM public.roommate_profiles" \
    "SELECT count(*) FROM roommate_profiles"
}

verify_listings() {
  compare_counts roommate_listings \
    "SELECT count(*) FROM public.roommate_listings" \
    "SELECT count(*) FROM roommate_listings"
  compare_counts roommate_listings_active_one_per_author \
    "SELECT count(DISTINCT author_user_id) FROM public.roommate_listings WHERE status = 'ACTIVE'" \
    "SELECT count(*) FROM roommate_listings WHERE status = 'ACTIVE'"
}

verify_matches() {
  compare_counts roommate_matches \
    "SELECT count(*) FROM public.roommate_matches" \
    "SELECT count(*) FROM roommate_matches"
  compare_counts roommate_matches_by_status \
    "SELECT string_agg(status || '=' || n, ',' ORDER BY status) FROM (SELECT status, count(*) AS n FROM public.roommate_matches GROUP BY status) s" \
    "SELECT string_agg(status || '=' || n, ',' ORDER BY status) FROM (SELECT status, count(*) AS n FROM roommate_matches GROUP BY status) s"
}

verify_participants() {
  compare_counts roommate_match_participants \
    "SELECT count(*) FROM public.roommate_match_participants" \
    "SELECT count(*) FROM roommate_match_participants"
  expect_zero roommate_match_participants_orphans \
    "SELECT count(*) FROM roommate_match_participants p WHERE NOT EXISTS (SELECT 1 FROM roommate_matches m WHERE m.id = p.match_id)"
}

verify_events() {
  compare_counts roommate_match_events \
    "SELECT count(*) FROM public.roommate_match_events" \
    "SELECT count(*) FROM roommate_match_events"
}

verify_viewings() {
  compare_counts roommate_viewings \
    "SELECT count(*) FROM public.roommate_viewings" \
    "SELECT count(*) FROM roommate_viewings"
  compare_counts roommate_viewings_with_snapshot \
    "SELECT count(*) FROM public.roommate_viewings v JOIN public.listings l ON l.id = v.listing_id" \
    "SELECT count(*) FROM roommate_viewings WHERE listing_id IS NOT NULL"
  compare_counts roommate_viewings_snapshot_price_sum \
    "SELECT COALESCE(sum(l.price), 0) FROM public.roommate_viewings v JOIN public.listings l ON l.id = v.listing_id" \
    "SELECT COALESCE(sum(listing_price), 0) FROM roommate_viewings"
  compare_counts roommate_viewings_snapshot_cover_photos \
    "SELECT count(*) FROM public.roommate_viewings v JOIN public.listings l ON l.id = v.listing_id WHERE array_length(l.photos, 1) > 0" \
    "SELECT count(listing_cover_photo_url) FROM roommate_viewings"
}

verify_shared_favorites() {
  compare_counts roommate_shared_favorites \
    "SELECT count(*) FROM public.roommate_shared_favorites f JOIN public.listing_status ls ON ls.id = f.listing_status_id WHERE ls.name IN ($SHARED_FAVORITE_STATUSES)" \
    "SELECT count(*) FROM roommate_shared_favorites"
  expect_zero roommate_shared_favorites_unknown_status \
    "SELECT count(*) FROM roommate_shared_favorites WHERE listing_status NOT IN ($SHARED_FAVORITE_STATUSES)"
}

verify_hidden() {
  compare_counts roommate_hidden \
    "SELECT count(*) FROM public.roommate_hidden" \
    "SELECT count(*) FROM roommate_hidden"
}

verify_moderation_tasks() {
  compare_counts roommate_moderation_tasks \
    "SELECT count(*) FROM public.roommate_moderation_tasks" \
    "SELECT count(*) FROM roommate_moderation_tasks"
}

main() {
  parse_args "$@"
  require_env MONOLITH_DB_URL TARGET_DB_URL
  run_step roommate_profiles verify_profiles
  run_step roommate_listings verify_listings
  run_step roommate_matches verify_matches
  run_step roommate_match_participants verify_participants
  run_step roommate_match_events verify_events
  run_step roommate_viewings verify_viewings
  run_step roommate_shared_favorites verify_shared_favorites
  run_step roommate_hidden verify_hidden
  run_step roommate_moderation_tasks verify_moderation_tasks
  log "verification finished"
}

main "$@"
