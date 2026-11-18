#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"

usage() {
  cat <<'USAGE'
Usage: migrate.sh [--dry-run]

Copies the identity domain from the monolith database (MONOLITH_DB_URL) into the
identity-ms database (TARGET_DB_URL): user_profiles, user_settings, user_filters, feedbacks.
Identifiers are preserved. Steps are idempotent: re-running does not duplicate rows.

Environment:
  MONOLITH_DB_URL   libpq URI of the monolith database
  TARGET_DB_URL     libpq URI of the identity-ms database

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

warn_count() {
  local label=$1 sql=$2 hint=$3 n
  n=$(dst_psql -At -c "$sql")
  if (( n > 0 )); then
    log "WARNING: [$label] $n row(s): $hint"
  fi
}

assert_bot_status_values() {
  local bad
  bad=$(dst_psql -At -c "SELECT DISTINCT bot_status FROM user_settings_stg WHERE bot_status NOT IN (SELECT enumlabel FROM pg_enum WHERE enumtypid = 'bot_status_enum'::regtype)" | paste -sd, -)
  if [[ -n $bad ]]; then
    die "user_settings.bot_status values [$bad] are missing in target bot_status_enum; add them with an identity-ms schema migration (ALTER TYPE bot_status_enum ADD VALUE ...) and re-run"
  fi
}

check_user_profiles_stg() {
  warn_count user_profiles "SELECT count(*) FROM user_profiles_stg WHERE telegram_id ~ '^[0-9]+\$'" \
    "telegram_id looks plaintext, not AES-GCM ciphertext; identity-ms cannot decrypt it"
  warn_count user_profiles "SELECT count(*) FROM user_profiles_stg WHERE telegram_id LIKE 'admin-seed-%'" \
    "seeded admin profile keeps a non-encrypted telegram_id"
  warn_count user_profiles "SELECT count(*) FROM user_profiles_stg WHERE telegram_id_hash IS NULL" \
    "telegram_id_hash is NULL; identity-ms entity maps it as NOT NULL and looks profiles up by it"
}

check_user_settings_stg() {
  assert_bot_status_values
  warn_count user_settings "SELECT count(*) FROM user_settings_stg WHERE refresh_token LIKE 'eyJ%'" \
    "refresh_token looks like a plaintext JWT, not AES-GCM ciphertext; identity-ms decrypt will fail"
}

migrate_user_profiles() {
  stage_copy user_profiles_stg \
    "SELECT id, created_at, updated_at, telegram_id, telegram_id_hash, username, last_login, photo_url FROM public.user_profiles ORDER BY id" \
    "id bigint, created_at timestamptz, updated_at timestamptz, telegram_id varchar(255), telegram_id_hash varchar(64), username varchar(255), last_login timestamptz, photo_url text"
  if ! is_dry_run; then
    check_user_profiles_stg
  fi
  dst_exec "INSERT INTO user_profiles (id, created_at, updated_at, telegram_id, telegram_id_hash, username, last_login, photo_url) SELECT id, created_at, updated_at, telegram_id, telegram_id_hash, username, last_login, photo_url FROM user_profiles_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence user_profiles id
  drop_staging user_profiles_stg
  compare_counts user_profiles \
    "SELECT count(*) FROM public.user_profiles" \
    "SELECT count(*) FROM user_profiles"
}

migrate_user_settings() {
  stage_copy user_settings_stg \
    "SELECT id, user_profile_id, email, phone_number, color_theme, language, refresh_token, password_hash, role_name, bot_status::text, quiet_from, quiet_to, is_email_active, created_at, updated_at FROM public.user_settings ORDER BY id" \
    "id bigint, user_profile_id bigint, email varchar(255), phone_number varchar(32), color_theme varchar(16), language varchar(8), refresh_token varchar(512), password_hash varchar(255), role_name varchar(50), bot_status text, quiet_from time, quiet_to time, is_email_active boolean, created_at timestamptz, updated_at timestamptz"
  if ! is_dry_run; then
    check_user_settings_stg
  fi
  dst_exec "INSERT INTO user_settings (id, user_profile_id, email, phone_number, color_theme, language, refresh_token, password_hash, role_name, bot_status, quiet_from, quiet_to, is_email_active, created_at, updated_at) SELECT id, user_profile_id, email, phone_number, color_theme, language, refresh_token, password_hash, role_name, bot_status::bot_status_enum, quiet_from, quiet_to, is_email_active, created_at, updated_at FROM user_settings_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence user_settings id
  drop_staging user_settings_stg
  compare_counts user_settings \
    "SELECT count(*) FROM public.user_settings" \
    "SELECT count(*) FROM user_settings"
}

migrate_user_filters() {
  stage_copy user_filters_stg \
    "SELECT id, user_profile_id, price_from, price_to, rooms_from, rooms_to, area_from, area_to, is_active, created_at, updated_at, currency, city, owner_match_id, category FROM public.user_filters ORDER BY id" \
    "id bigint, user_profile_id bigint, price_from integer, price_to integer, rooms_from integer, rooms_to integer, area_from double precision, area_to double precision, is_active boolean, created_at timestamptz, updated_at timestamptz, currency varchar(5), city varchar(255), owner_match_id bigint, category varchar(10)"
  dst_exec "INSERT INTO user_filters (id, user_profile_id, price_from, price_to, rooms_from, rooms_to, area_from, area_to, is_active, created_at, updated_at, currency, city, owner_match_id, category) SELECT s.id, s.user_profile_id, s.price_from, s.price_to, s.rooms_from, s.rooms_to, s.area_from, s.area_to, s.is_active, s.created_at, s.updated_at, s.currency, s.city, s.owner_match_id, s.category FROM user_filters_stg s WHERE NOT EXISTS (SELECT 1 FROM user_filters t WHERE t.id = s.id) ON CONFLICT (id) DO NOTHING"
  reset_sequence user_filters id
  drop_staging user_filters_stg
  compare_counts user_filters \
    "SELECT count(*) FROM public.user_filters" \
    "SELECT count(*) FROM user_filters"
}

migrate_feedbacks() {
  stage_copy feedbacks_stg \
    "SELECT id, user_id, message, rating, created_at, updated_at, subject_type, subject_id, reason_code FROM public.feedbacks ORDER BY id" \
    "id bigint, user_id bigint, message varchar(2000), rating integer, created_at timestamptz, updated_at timestamptz, subject_type varchar(20), subject_id bigint, reason_code varchar(20)"
  dst_exec "INSERT INTO feedbacks (id, user_id, message, rating, created_at, updated_at, subject_type, subject_id, reason_code) SELECT id, user_id, message, rating, created_at, updated_at, subject_type, subject_id, reason_code FROM feedbacks_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence feedbacks id
  drop_staging feedbacks_stg
  compare_counts feedbacks \
    "SELECT count(*) FROM public.feedbacks" \
    "SELECT count(*) FROM feedbacks"
}

main() {
  parse_args "$@"
  require_env MONOLITH_DB_URL TARGET_DB_URL
  log "crypto: telegram_id, telegram_id_hash and refresh_token are copied as-is (AES-GCM iv||ciphertext base64, HMAC-SHA256 base64 - formats match in both services); identity-ms must run with the same app.crypto.aes-key and app.crypto.hmac-key as the monolith, otherwise these values need re-encryption"
  run_step user_profiles migrate_user_profiles
  run_step user_settings migrate_user_settings
  run_step user_filters migrate_user_filters
  run_step feedbacks migrate_feedbacks
  log "migration finished"
}

main "$@"
