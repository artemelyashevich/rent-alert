#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"

SHARED_FAVORITE_STATUSES="'INTERESTED','WAITING_RESPONSE','VIEWING_SCHEDULED','RESERVED','VIEWED','AGREED'"

usage() {
  cat <<'USAGE'
Usage: migrate.sh [--dry-run]

Copies roommate_* data from the monolith database (MONOLITH_DB_URL) into the
roommate-ms database (TARGET_DB_URL). Rows keep their original ids; tables are
copied in foreign-key order. Re-running does not duplicate rows.

Preflight (before any write) reports and fixes constraint violators:
  roommate_listings        more than one ACTIVE listing per author: the most recently
                           updated one stays ACTIVE, the others are copied as PAUSED
  roommate_match_participants  more than one committed row per user and scenario: the most
                           recently updated one stays committed, the others are copied as not committed
  roommate_matches         user_lo_id >= user_hi_id aborts the run (no automatic fix)
  roommate_shared_favorites  listing statuses without a ListingStatusName match are skipped with a warning

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

report_active_listing_duplicates() {
  local rows author count ids kept
  rows=$(src_psql -At -F '|' -c "SELECT author_user_id, count(*), string_agg(id::text, ',' ORDER BY id), (array_agg(id ORDER BY updated_at DESC, id DESC))[1] FROM public.roommate_listings WHERE status = 'ACTIVE' GROUP BY author_user_id HAVING count(*) > 1 ORDER BY author_user_id")
  if [[ -z $rows ]]; then
    log "[preflight] roommate_listings: at most one ACTIVE listing per author, no violations"
    return 0
  fi
  while IFS='|' read -r author count ids kept; do
    log "WARNING: roommate_listings author_user_id=$author has $count ACTIVE listings ($ids); keeping ACTIVE id=$kept, others are copied as PAUSED"
  done <<< "$rows"
}

report_committed_participant_duplicates() {
  local rows user scenario count ids kept
  rows=$(src_psql -At -F '|' -c "SELECT user_id, scenario, count(*), string_agg(id::text, ',' ORDER BY id), (array_agg(id ORDER BY updated_at DESC, id DESC))[1] FROM public.roommate_match_participants WHERE committed GROUP BY user_id, scenario HAVING count(*) > 1 ORDER BY user_id, scenario")
  if [[ -z $rows ]]; then
    log "[preflight] roommate_match_participants: at most one committed row per user and scenario, no violations"
    return 0
  fi
  while IFS='|' read -r user scenario count ids kept; do
    log "WARNING: roommate_match_participants user_id=$user scenario=$scenario has $count committed rows ($ids); keeping committed id=$kept, others are copied as not committed"
  done <<< "$rows"
}

check_match_pair_order() {
  local ids
  ids=$(src_psql -At -c "SELECT string_agg(id::text, ',' ORDER BY id) FROM public.roommate_matches WHERE user_lo_id >= user_hi_id")
  if [[ -n $ids ]]; then
    die "roommate_matches violates user_lo_id < user_hi_id, match ids: $ids (fix in the monolith before migrating)"
  fi
  log "[preflight] roommate_matches: user_lo_id < user_hi_id holds for all rows"
}

report_unmapped_shared_favorites() {
  local rows name count
  rows=$(src_psql -At -F '|' -c "SELECT ls.name, count(*) FROM public.roommate_shared_favorites f JOIN public.listing_status ls ON ls.id = f.listing_status_id WHERE ls.name NOT IN ($SHARED_FAVORITE_STATUSES) GROUP BY ls.name ORDER BY ls.name")
  if [[ -z $rows ]]; then
    log "[preflight] roommate_shared_favorites: every listing status maps to ListingStatusName"
    return 0
  fi
  while IFS='|' read -r name count; do
    log "WARNING: roommate_shared_favorites status '$name' has no ListingStatusName match, $count rows will be skipped"
  done <<< "$rows"
}

preflight_roommate() {
  report_active_listing_duplicates
  report_committed_participant_duplicates
  check_match_pair_order
  report_unmapped_shared_favorites
}

migrate_profiles() {
  stage_copy roommate_profiles_stg \
    "SELECT user_id, gender, gender_pref::varchar[], birth_year, budget_min, budget_max, currency, city, move_in_from, move_in_to, lifestyle_tags::varchar[], schedule, smoking, pets, guests, cleanliness, bio_submitted, bio_approved, bio_mod_status, is_active, is_searchable, profile_version, reports_count, last_digest_at, created_at, updated_at FROM public.roommate_profiles ORDER BY user_id" \
    "LIKE roommate_profiles"
  dst_exec "INSERT INTO roommate_profiles SELECT * FROM roommate_profiles_stg ON CONFLICT (user_id) DO NOTHING"
  drop_staging roommate_profiles_stg
  compare_counts roommate_profiles \
    "SELECT count(*) FROM public.roommate_profiles" \
    "SELECT count(*) FROM roommate_profiles"
}

migrate_listings() {
  stage_copy roommate_listings_stg \
    "WITH kept_active AS (SELECT DISTINCT ON (author_user_id) id FROM public.roommate_listings WHERE status = 'ACTIVE' ORDER BY author_user_id, updated_at DESC, id DESC) SELECT r.id, r.author_user_id, r.listing_id, r.city, r.address_exact, r.address_public, r.district, r.metro_station, r.geo_lat, r.geo_lng, r.rent_share, r.currency, r.room_count, r.flatmates_count, r.available_from, r.house_rules::varchar[], r.description_submitted, r.description_approved, r.description_mod_status, CASE WHEN r.status = 'ACTIVE' AND r.id NOT IN (SELECT id FROM kept_active) THEN 'PAUSED' ELSE r.status END, r.gender_pref::varchar[], r.age_min, r.age_max, r.reports_count, r.created_at, r.updated_at FROM public.roommate_listings r ORDER BY r.id" \
    "LIKE roommate_listings"
  dst_exec "INSERT INTO roommate_listings SELECT * FROM roommate_listings_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence roommate_listings id
  drop_staging roommate_listings_stg
  compare_counts roommate_listings \
    "SELECT count(*) FROM public.roommate_listings" \
    "SELECT count(*) FROM roommate_listings"
}

migrate_matches() {
  stage_copy roommate_matches_stg \
    "SELECT id, scenario, user_lo_id, user_hi_id, context_ref, roommate_listing_id, lo_decision, hi_decision, status, status_version, initiator_user_id, contacts_unlocked_at, chat_ref, expires_at, lo_moved, hi_moved, created_at, updated_at FROM public.roommate_matches ORDER BY id" \
    "LIKE roommate_matches"
  dst_exec "INSERT INTO roommate_matches SELECT * FROM roommate_matches_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence roommate_matches id
  drop_staging roommate_matches_stg
  compare_counts roommate_matches \
    "SELECT count(*) FROM public.roommate_matches" \
    "SELECT count(*) FROM roommate_matches"
}

migrate_participants() {
  stage_copy roommate_match_participants_stg \
    "WITH kept_committed AS (SELECT DISTINCT ON (user_id, scenario) id FROM public.roommate_match_participants WHERE committed ORDER BY user_id, scenario, updated_at DESC, id DESC) SELECT p.id, p.match_id, p.user_id, p.scenario, p.role, (p.committed AND p.id IN (SELECT id FROM kept_committed)) AS committed, p.created_at, p.updated_at FROM public.roommate_match_participants p ORDER BY p.id" \
    "LIKE roommate_match_participants"
  dst_exec "INSERT INTO roommate_match_participants SELECT * FROM roommate_match_participants_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence roommate_match_participants id
  drop_staging roommate_match_participants_stg
  compare_counts roommate_match_participants \
    "SELECT count(*) FROM public.roommate_match_participants" \
    "SELECT count(*) FROM roommate_match_participants"
}

migrate_events() {
  stage_copy roommate_match_events_stg \
    "SELECT id, match_id, from_status, to_status, actor_user_id, note, created_at, updated_at FROM public.roommate_match_events ORDER BY id" \
    "LIKE roommate_match_events"
  dst_exec "INSERT INTO roommate_match_events SELECT * FROM roommate_match_events_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence roommate_match_events id
  drop_staging roommate_match_events_stg
  compare_counts roommate_match_events \
    "SELECT count(*) FROM public.roommate_match_events" \
    "SELECT count(*) FROM roommate_match_events"
}

migrate_viewings() {
  stage_copy roommate_viewings_stg \
    "SELECT v.id, v.match_id, v.listing_id, v.address, v.viewing_date, v.created_by_user_id, v.status, l.title, l.street, l.metro_station, l.price, l.currency, CASE WHEN array_length(l.photos, 1) > 0 THEN l.photos[1] END, v.created_at, v.updated_at FROM public.roommate_viewings v LEFT JOIN public.listings l ON l.id = v.listing_id ORDER BY v.id" \
    "LIKE roommate_viewings"
  dst_exec "INSERT INTO roommate_viewings SELECT * FROM roommate_viewings_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence roommate_viewings id
  drop_staging roommate_viewings_stg
  compare_counts roommate_viewings \
    "SELECT count(*) FROM public.roommate_viewings" \
    "SELECT count(*) FROM roommate_viewings"
}

migrate_shared_favorites() {
  stage_copy roommate_shared_favorites_stg \
    "SELECT f.match_id, f.listing_id, ls.name, f.viewing_date, f.added_by_user_id, f.created_at, f.updated_at FROM public.roommate_shared_favorites f JOIN public.listing_status ls ON ls.id = f.listing_status_id WHERE ls.name IN ($SHARED_FAVORITE_STATUSES) ORDER BY f.match_id, f.listing_id" \
    "LIKE roommate_shared_favorites"
  dst_exec "INSERT INTO roommate_shared_favorites SELECT * FROM roommate_shared_favorites_stg ON CONFLICT (match_id, listing_id) DO NOTHING"
  drop_staging roommate_shared_favorites_stg
  compare_counts roommate_shared_favorites \
    "SELECT count(*) FROM public.roommate_shared_favorites f JOIN public.listing_status ls ON ls.id = f.listing_status_id WHERE ls.name IN ($SHARED_FAVORITE_STATUSES)" \
    "SELECT count(*) FROM roommate_shared_favorites"
}

migrate_hidden() {
  stage_copy roommate_hidden_stg \
    "SELECT user_id, subject_type, subject_id, created_at FROM public.roommate_hidden ORDER BY user_id, subject_type, subject_id" \
    "LIKE roommate_hidden"
  dst_exec "INSERT INTO roommate_hidden SELECT * FROM roommate_hidden_stg ON CONFLICT (user_id, subject_type, subject_id) DO NOTHING"
  drop_staging roommate_hidden_stg
  compare_counts roommate_hidden \
    "SELECT count(*) FROM public.roommate_hidden" \
    "SELECT count(*) FROM roommate_hidden"
}

migrate_moderation_tasks() {
  stage_copy roommate_moderation_tasks_stg \
    "SELECT id, subject_type, subject_id, field, submitted_text, text_hash, status, reviewer_id, decision_reason, created_at, updated_at FROM public.roommate_moderation_tasks ORDER BY id" \
    "LIKE roommate_moderation_tasks"
  dst_exec "INSERT INTO roommate_moderation_tasks SELECT * FROM roommate_moderation_tasks_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence roommate_moderation_tasks id
  drop_staging roommate_moderation_tasks_stg
  compare_counts roommate_moderation_tasks \
    "SELECT count(*) FROM public.roommate_moderation_tasks" \
    "SELECT count(*) FROM roommate_moderation_tasks"
}

main() {
  parse_args "$@"
  require_env MONOLITH_DB_URL TARGET_DB_URL
  run_step preflight preflight_roommate
  run_step roommate_profiles migrate_profiles
  run_step roommate_listings migrate_listings
  run_step roommate_matches migrate_matches
  run_step roommate_match_participants migrate_participants
  run_step roommate_match_events migrate_events
  run_step roommate_viewings migrate_viewings
  run_step roommate_shared_favorites migrate_shared_favorites
  run_step roommate_hidden migrate_hidden
  run_step roommate_moderation_tasks migrate_moderation_tasks
  log "migration finished"
}

main "$@"
