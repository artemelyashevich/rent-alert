#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"

usage() {
  cat <<'USAGE'
Usage: migrate.sh [--dry-run]

Copies the notification domain from the monolith database (MONOLITH_DB_URL) into the
notification-ms database (TARGET_DB_URL): notifications, notification_broadcasts,
notification_broadcast_recipients, user_notification_settings, telegram_messages, reminders.
Steps are idempotent: rows are inserted with ON CONFLICT (id) DO NOTHING.

Not copied: notification_templates and notification_type_settings (seeded by V005, V007, V010
of this service), outbox_event and inbox_event (no monolith source).

Environment:
  MONOLITH_DB_URL   libpq URI of the monolith database
  TARGET_DB_URL     libpq URI of the notification-ms database
  MIGRATE_SINCE     optional ISO-8601 timestamp (e.g. 2026-01-01 or 2026-01-01T00:00:00Z). When set,
                    journal tables copy only rows with created_at >= MIGRATE_SINCE
                    (notifications, telegram_messages, notification_broadcasts; recipients follow their
                    broadcast). Unset: everything.

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

validate_since() {
  local value=${MIGRATE_SINCE:-}
  local re='^[0-9]{4}-[0-9]{2}-[0-9]{2}([T ][0-9]{2}:[0-9]{2}(:[0-9]{2}(\.[0-9]{1,6})?)?(Z|[+-][0-9]{2}(:?[0-9]{2})?)?)?$'
  if [[ -z $value ]]; then
    return 0
  fi
  [[ $value =~ $re ]] || die "MIGRATE_SINCE is not an ISO-8601 timestamp: '$value'"
}

since_and() {
  if [[ -n ${MIGRATE_SINCE:-} ]]; then
    printf " AND %s >= '%s'::timestamptz" "$1" "$MIGRATE_SINCE"
  fi
}

in_list() {
  local out="" value
  for value in "$@"; do
    out+="${out:+,}'$value'"
  done
  printf '%s' "$out"
}

assert_zero() {
  local label=$1 sql=$2 count
  count=$(src_count_query "$sql")
  if [[ $count != 0 ]]; then
    die "[$label] $count monolith row(s) violate the notification-ms schema; fix the source before migrating"
  fi
  log "[$label] source precheck OK"
}

enum_check() {
  local label=$1 table=$2 column=$3 since_column=$4
  shift 4
  local where="TRUE"
  if [[ -n $since_column ]]; then
    where+="$(since_and "$since_column")"
  fi
  assert_zero "$label" "SELECT count(*) FROM public.$table WHERE $where AND $column NOT IN ($(in_list "$@"))"
}

ENGINES=(TELEGRAM_ADMIN EMAIL PUSH TELEGRAM_BOT WEBSOCKET SMS)
NOTIFICATION_STATUSES=(PENDING SUCCESS FAILURE EXHAUSTED QUEUED)
NOTIFICATION_TYPES=(APP_STARTED APP_STOPPED NEW_LISTING_ADMIN_ALERT USER_CREATED NEW_LISTING_MATCHED FAVORITE_REMINDER EMAIL_ACTIVATION EMAIL_ACTIVATION_SUCCESS NEW_FEEDBACK BOT_BLOCKED BOT_UNBLOCKED ADMIN_BROADCAST ROOMMATE_NEW_INTEREST ROOMMATE_NEW_CANDIDATES ROOMMATE_MATCHED ROOMMATE_CHAT_STARTED ROOMMATE_VIEWING_SCHEDULED ROOMMATE_VIEWING_REMINDER ROOMMATE_MATCH_ENDED ROOMMATE_INTEREST_EXPIRED ROOMMATE_PROFILE_APPROVED ROOMMATE_PROFILE_REJECTED)
REMINDER_STATUSES=(NEW PROCESSING SENT FAILED CANCELLED)
REMINDER_INTERVALS=(BEFORE_30_MIN BEFORE_1_HOUR BEFORE_1_DAY)
BROADCAST_RECIPIENT_STATUSES=(SENT FAILED SKIPPED)
TELEGRAM_COMMANDS=(START APP HELP)

migrate_notification_broadcasts() {
  stage_copy notification_broadcasts_stg \
    "SELECT id, created_by, message, total_recipients, sent_count, failed_count, skipped_count, created_at, updated_at, pinned FROM public.notification_broadcasts WHERE TRUE$(since_and created_at) ORDER BY id" \
    "id bigint, created_by bigint, message text, total_recipients integer, sent_count integer, failed_count integer, skipped_count integer, created_at timestamptz, updated_at timestamptz, pinned boolean"
  dst_exec "INSERT INTO notification_broadcasts (id, created_by, message, total_recipients, sent_count, failed_count, skipped_count, created_at, updated_at, pinned) SELECT id, created_by, message, total_recipients, sent_count, failed_count, skipped_count, created_at, updated_at, pinned FROM notification_broadcasts_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence notification_broadcasts id
  drop_staging notification_broadcasts_stg
  compare_counts notification_broadcasts \
    "SELECT count(*) FROM public.notification_broadcasts WHERE TRUE$(since_and created_at)" \
    "SELECT count(*) FROM notification_broadcasts WHERE TRUE$(since_and created_at)"
}

migrate_notification_broadcast_recipients() {
  enum_check notification_broadcast_recipients_status_precheck notification_broadcast_recipients status "" "${BROADCAST_RECIPIENT_STATUSES[@]}"
  stage_copy notification_broadcast_recipients_stg \
    "SELECT r.id, r.broadcast_id, r.user_id, r.status, r.error_message, r.created_at, r.updated_at FROM public.notification_broadcast_recipients r JOIN public.notification_broadcasts b ON b.id = r.broadcast_id WHERE TRUE$(since_and b.created_at) ORDER BY r.id" \
    "id bigint, broadcast_id bigint, user_id bigint, status varchar(50), error_message text, created_at timestamptz, updated_at timestamptz"
  dst_exec "INSERT INTO notification_broadcast_recipients (id, broadcast_id, user_id, status, error_message, created_at, updated_at) SELECT id, broadcast_id, user_id, status, error_message, created_at, updated_at FROM notification_broadcast_recipients_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence notification_broadcast_recipients id
  drop_staging notification_broadcast_recipients_stg
  compare_counts notification_broadcast_recipients \
    "SELECT count(*) FROM public.notification_broadcast_recipients r JOIN public.notification_broadcasts b ON b.id = r.broadcast_id WHERE TRUE$(since_and b.created_at)" \
    "SELECT count(*) FROM notification_broadcast_recipients r JOIN notification_broadcasts b ON b.id = r.broadcast_id WHERE TRUE$(since_and b.created_at)"
}

migrate_user_notification_settings() {
  enum_check user_notification_settings_engine_precheck user_notification_settings engine "" "${ENGINES[@]}"
  stage_copy user_notification_settings_stg \
    "SELECT id, user_id, engine, is_enabled, is_default, created_at, updated_at FROM public.user_notification_settings ORDER BY id" \
    "id bigint, user_id bigint, engine varchar(50), is_enabled boolean, is_default boolean, created_at timestamp, updated_at timestamp"
  dst_exec "INSERT INTO user_notification_settings (id, user_id, engine, is_enabled, is_default, created_at, updated_at) SELECT id, user_id, engine, is_enabled, is_default, created_at, updated_at FROM user_notification_settings_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence user_notification_settings id
  drop_staging user_notification_settings_stg
  compare_counts user_notification_settings \
    "SELECT count(*) FROM public.user_notification_settings" \
    "SELECT count(*) FROM user_notification_settings"
}

migrate_telegram_messages() {
  enum_check telegram_messages_command_precheck telegram_messages command created_at "${TELEGRAM_COMMANDS[@]}"
  stage_copy telegram_messages_stg \
    "SELECT id, user_id, text, command, created_at, updated_at FROM public.telegram_messages WHERE TRUE$(since_and created_at) ORDER BY id" \
    "id bigint, user_id bigint, text text, command varchar(255), created_at timestamptz, updated_at timestamptz"
  dst_exec "INSERT INTO telegram_messages (id, user_id, text, command, created_at, updated_at) SELECT id, user_id, text, command, created_at, updated_at FROM telegram_messages_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence telegram_messages id
  drop_staging telegram_messages_stg
  compare_counts telegram_messages \
    "SELECT count(*) FROM public.telegram_messages WHERE TRUE$(since_and created_at)" \
    "SELECT count(*) FROM telegram_messages WHERE TRUE$(since_and created_at)"
}

migrate_notifications() {
  enum_check notifications_engine_precheck notifications engine created_at "${ENGINES[@]}"
  enum_check notifications_status_precheck notifications status created_at "${NOTIFICATION_STATUSES[@]}"
  enum_check notifications_retry_type_precheck notifications retry_type created_at "${NOTIFICATION_TYPES[@]}"
  assert_zero notifications_success_dedup_precheck "SELECT count(*) FROM (SELECT deduplication_key, engine FROM public.notifications WHERE status = 'SUCCESS' AND deduplication_key IS NOT NULL$(since_and created_at) GROUP BY deduplication_key, engine HAVING count(*) > 1) d"
  stage_copy notifications_stg \
    "SELECT id, engine, receiver, user_id, content, status, created_at, updated_at, attempt, error_message, next_retry_at, deduplication_key, retry_type, template_params_json, listing_id FROM public.notifications WHERE TRUE$(since_and created_at) ORDER BY id" \
    "id bigint, engine varchar(50), receiver varchar(255), user_id bigint, content text, status varchar(50), created_at timestamptz, updated_at timestamptz, attempt integer, error_message text, next_retry_at timestamptz, deduplication_key varchar(255), retry_type text, template_params_json text, listing_id bigint"
  dst_exec "INSERT INTO notifications (id, engine, receiver, user_id, content, status, created_at, updated_at, attempt, error_message, next_retry_at, deduplication_key, retry_type, template_params_json, listing_id) SELECT id, engine, receiver, user_id, content, status, created_at, updated_at, attempt, error_message, next_retry_at, deduplication_key, retry_type, template_params_json, listing_id FROM notifications_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence notifications id
  drop_staging notifications_stg
  compare_counts notifications \
    "SELECT count(*) FROM public.notifications WHERE TRUE$(since_and created_at)" \
    "SELECT count(*) FROM notifications WHERE TRUE$(since_and created_at)"
}

migrate_reminders() {
  enum_check reminders_status_precheck reminders status "" "${REMINDER_STATUSES[@]}"
  enum_check reminders_interval_precheck reminders interval_type "" "${REMINDER_INTERVALS[@]}"
  stage_copy reminders_stg \
    "SELECT id, user_id, listing_id, roommate_viewing_id, remind_at, status, notification_template_id, interval_type, sent_at, attempts, error, created_at, updated_at FROM public.reminders ORDER BY id" \
    "id bigint, user_id bigint, listing_id bigint, roommate_viewing_id bigint, remind_at timestamptz, status varchar(50), notification_template_id bigint, interval_type varchar(50), sent_at timestamptz, attempts integer, error text, created_at timestamptz, updated_at timestamptz"
  stage_copy notification_template_map_stg \
    "SELECT id, notification_type, engine FROM public.notification_templates" \
    "id bigint, notification_type text, engine text"
  if ! is_dry_run; then
    local unmapped
    unmapped=$(dst_psql -At -c "SELECT count(*) FROM reminders_stg s LEFT JOIN notification_template_map_stg m ON m.id = s.notification_template_id LEFT JOIN notification_templates t ON t.notification_type = m.notification_type AND t.engine = m.engine WHERE s.notification_template_id IS NOT NULL AND t.id IS NULL")
    if (( unmapped > 0 )); then
      log "WARNING: $unmapped reminder(s) reference a monolith template with no (notification_type, engine) match in notification-ms; notification_template_id is left NULL for them"
    fi
  fi
  dst_exec "INSERT INTO reminders (id, user_id, listing_id, roommate_viewing_id, remind_at, status, notification_template_id, interval_type, sent_at, attempts, error, created_at, updated_at) SELECT s.id, s.user_id, s.listing_id, s.roommate_viewing_id, s.remind_at, s.status, t.id, s.interval_type, s.sent_at, s.attempts, s.error, s.created_at, s.updated_at FROM reminders_stg s LEFT JOIN notification_template_map_stg m ON m.id = s.notification_template_id LEFT JOIN notification_templates t ON t.notification_type = m.notification_type AND t.engine = m.engine ORDER BY s.id ON CONFLICT (id) DO NOTHING"
  reset_sequence reminders id
  drop_staging notification_template_map_stg
  drop_staging reminders_stg
  compare_counts reminders \
    "SELECT count(*) FROM public.reminders" \
    "SELECT count(*) FROM reminders"
  compare_counts reminders_with_template \
    "SELECT count(*) FROM public.reminders WHERE notification_template_id IS NOT NULL" \
    "SELECT count(*) FROM reminders WHERE notification_template_id IS NOT NULL"
}

main() {
  parse_args "$@"
  require_env MONOLITH_DB_URL TARGET_DB_URL
  validate_since
  if [[ -n ${MIGRATE_SINCE:-} ]]; then
    log "MIGRATE_SINCE=$MIGRATE_SINCE: journal tables copy rows created since this moment"
  fi
  log "WARNING: receiver (AES-GCM, base64, IV prefix; written encrypted only for TELEGRAM_BOT SUCCESS rows, plaintext otherwise) and deduplication_key (HMAC-SHA256, base64) are copied as stored. The formats match the monolith, so no re-encryption is needed, but notification-ms must run with the same app.crypto.aes-key and app.crypto.hmac-key as the monolith; otherwise receivers cannot be decrypted and deduplication keys stop matching"
  run_step notification_broadcasts migrate_notification_broadcasts
  run_step notification_broadcast_recipients migrate_notification_broadcast_recipients
  run_step user_notification_settings migrate_user_notification_settings
  run_step telegram_messages migrate_telegram_messages
  run_step notifications migrate_notifications
  run_step reminders migrate_reminders
  log "migration finished"
}

main "$@"
