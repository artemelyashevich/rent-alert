#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"

usage() {
  cat <<'USAGE'
Usage: verify.sh

Compares row counts and control aggregates between the monolith database (MONOLITH_DB_URL)
and the notification-ms database (TARGET_DB_URL). Read-only. Exits non-zero on any MISMATCH.
Template drift (notification_templates text differing from the monolith) is reported as WARN only.

Environment:
  MONOLITH_DB_URL   libpq URI of the monolith database
  TARGET_DB_URL     libpq URI of the notification-ms database
  MIGRATE_SINCE     optional ISO-8601 timestamp; must be the same value that was used for migrate.sh

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

compare_aggregate() {
  local label=$1 src_sql=$2 dst_sql=$3 src dst
  src=$(src_psql -At -c "$src_sql")
  dst=$(dst_psql -At -c "$dst_sql")
  if [[ $src == "$dst" ]]; then
    log "[$label] OK"
    return 0
  fi
  log "[$label] MISMATCH"
  printf 'source:\n%s\ntarget:\n%s\n' "$src" "$dst"
  return 1
}

template_drift() {
  local src dst diffs
  src=$(src_psql -At -c "SELECT notification_type || '|' || engine || '|' || md5(template_text) FROM public.notification_templates ORDER BY 1")
  dst=$(dst_psql -At -c "SELECT notification_type || '|' || engine || '|' || md5(template_text) FROM notification_templates ORDER BY 1")
  diffs=$(comm -3 <(printf '%s\n' "$src" | sed '/^$/d' | sort) <(printf '%s\n' "$dst" | sed '/^$/d' | sort) || true)
  if [[ -z $diffs ]]; then
    log "[notification_templates_drift] OK (no template text differs from the monolith)"
  else
    log "WARN: [notification_templates_drift] template text differs between monolith and notification-ms (left column: monolith only or differing; review manually, not copied by design)"
    printf '%s\n' "$diffs"
  fi
}

verify_notification_broadcasts() {
  compare_counts notification_broadcasts \
    "SELECT count(*) FROM public.notification_broadcasts WHERE TRUE$(since_and created_at)" \
    "SELECT count(*) FROM notification_broadcasts WHERE TRUE$(since_and created_at)"
  compare_counts notification_broadcast_recipients \
    "SELECT count(*) FROM public.notification_broadcast_recipients r JOIN public.notification_broadcasts b ON b.id = r.broadcast_id WHERE TRUE$(since_and b.created_at)" \
    "SELECT count(*) FROM notification_broadcast_recipients r JOIN notification_broadcasts b ON b.id = r.broadcast_id WHERE TRUE$(since_and b.created_at)"
  compare_aggregate notification_broadcast_recipients_by_status \
    "SELECT r.status || '=' || count(*) FROM public.notification_broadcast_recipients r JOIN public.notification_broadcasts b ON b.id = r.broadcast_id WHERE TRUE$(since_and b.created_at) GROUP BY r.status ORDER BY 1" \
    "SELECT r.status || '=' || count(*) FROM notification_broadcast_recipients r JOIN notification_broadcasts b ON b.id = r.broadcast_id WHERE TRUE$(since_and b.created_at) GROUP BY r.status ORDER BY 1"
}

verify_user_notification_settings() {
  compare_counts user_notification_settings \
    "SELECT count(*) FROM public.user_notification_settings" \
    "SELECT count(*) FROM user_notification_settings"
  compare_aggregate user_notification_settings_by_engine \
    "SELECT engine || '=' || count(*) FILTER (WHERE is_enabled) || '/' || count(*) FROM public.user_notification_settings GROUP BY engine ORDER BY 1" \
    "SELECT engine || '=' || count(*) FILTER (WHERE is_enabled) || '/' || count(*) FROM user_notification_settings GROUP BY engine ORDER BY 1"
}

verify_telegram_messages() {
  compare_counts telegram_messages \
    "SELECT count(*) FROM public.telegram_messages WHERE TRUE$(since_and created_at)" \
    "SELECT count(*) FROM telegram_messages WHERE TRUE$(since_and created_at)"
}

verify_notifications() {
  compare_counts notifications \
    "SELECT count(*) FROM public.notifications WHERE TRUE$(since_and created_at)" \
    "SELECT count(*) FROM notifications WHERE TRUE$(since_and created_at)"
  compare_aggregate notifications_by_status \
    "SELECT status || '=' || count(*) FROM public.notifications WHERE TRUE$(since_and created_at) GROUP BY status ORDER BY 1" \
    "SELECT status || '=' || count(*) FROM notifications WHERE TRUE$(since_and created_at) GROUP BY status ORDER BY 1"
  compare_aggregate notifications_by_engine_status \
    "SELECT engine || '/' || status || '=' || count(*) FROM public.notifications WHERE TRUE$(since_and created_at) GROUP BY engine, status ORDER BY 1" \
    "SELECT engine || '/' || status || '=' || count(*) FROM notifications WHERE TRUE$(since_and created_at) GROUP BY engine, status ORDER BY 1"
}

verify_reminders() {
  compare_counts reminders \
    "SELECT count(*) FROM public.reminders" \
    "SELECT count(*) FROM reminders"
  compare_counts reminders_with_template \
    "SELECT count(*) FROM public.reminders WHERE notification_template_id IS NOT NULL" \
    "SELECT count(*) FROM reminders WHERE notification_template_id IS NOT NULL"
  compare_counts reminders_favorite \
    "SELECT count(*) FROM public.reminders WHERE roommate_viewing_id IS NULL" \
    "SELECT count(*) FROM reminders WHERE roommate_viewing_id IS NULL"
  compare_counts reminders_roommate_viewing \
    "SELECT count(*) FROM public.reminders WHERE roommate_viewing_id IS NOT NULL" \
    "SELECT count(*) FROM reminders WHERE roommate_viewing_id IS NOT NULL"
  compare_aggregate reminders_by_status \
    "SELECT status || '=' || count(*) FROM public.reminders GROUP BY status ORDER BY 1" \
    "SELECT status || '=' || count(*) FROM reminders GROUP BY status ORDER BY 1"
}

main() {
  parse_args "$@"
  require_env MONOLITH_DB_URL TARGET_DB_URL
  validate_since
  run_step notification_broadcasts verify_notification_broadcasts
  run_step user_notification_settings verify_user_notification_settings
  run_step telegram_messages verify_telegram_messages
  run_step notifications verify_notifications
  run_step reminders verify_reminders
  run_step notification_templates_drift template_drift
  log "verification finished"
}

main "$@"
