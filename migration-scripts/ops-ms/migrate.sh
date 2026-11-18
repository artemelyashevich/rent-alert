#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"

usage() {
  cat <<'USAGE'
Usage: migrate.sh [--dry-run]

Copies the ops domain from the monolith database (MONOLITH_DB_URL) into the ops-ms database
(TARGET_DB_URL): audit_log, job_setting (job history), user_statistics (user events).
Steps are idempotent: rows are inserted with ON CONFLICT (id) DO NOTHING.

Not copied (no monolith source): cleanup_saga and cleanup_saga_step (cleanup saga history starts
in ops-ms), outbox_event and inbox_event (transient Kafka state).

Environment:
  MONOLITH_DB_URL   libpq URI of the monolith database
  TARGET_DB_URL     libpq URI of the ops-ms database
  MIGRATE_SINCE     optional ISO-8601 timestamp (e.g. 2026-01-01 or 2026-01-01T00:00:00Z). When set,
                    audit_log, job_setting and user_statistics copy only rows with created_at >= MIGRATE_SINCE.
                    Unset: everything.

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
    die "[$label] $count monolith row(s) violate the ops-ms schema; fix the source before migrating"
  fi
  log "[$label] source precheck OK"
}

enum_check() {
  local label=$1 table=$2 column=$3
  shift 3
  assert_zero "$label" "SELECT count(*) FROM public.$table WHERE TRUE$(since_and created_at) AND $column NOT IN ($(in_list "$@"))"
}

EVENT_INITIATORS=(INTERNAL USER ADMIN)
EVENT_STATUSES=(SUCCESS ERROR)
JOB_NAMES=(AUDIT_CLEANUP CLEANUP CACHE_SYNC CACHE_CLEAN CURRENCY_RATE_SYNC)
JOB_INITIATORS=(INTERNAL ADMIN)
JOB_SCOPES=(DB CACHE CACHE_DB_SYNC)
JOB_STATUSES=(SUCCESS FAILED)
USER_EVENTS=(LISTING_RECEIVED FAVORITE_ADDED FAVORITE_REMOVED FILTER_UPDATED FEEDBACK_CREATED LOGIN_SUCCESS LOGIN_FAILED PROFILE_UPDATED USER_DELETED USER_REGISTERED FILTER_CREATED FILTER_DELETED FILTER_ACTIVATED HIDDEN_LISTING NOTIFICATION_SETTINGS_UPDATED BOT_COMMAND TELEGRAM_CALLBACK CALLBACK_FAVORITE CALLBACK_UNFAVORITE CALLBACK_HIDE BOT_BLOCKED BOT_UNBLOCKED ROOMMATE_CONTACT_REVEALED)

migrate_audit_log() {
  enum_check audit_log_event_initiator_precheck audit_log event_initiator "${EVENT_INITIATORS[@]}"
  enum_check audit_log_event_status_precheck audit_log event_status "${EVENT_STATUSES[@]}"
  stage_copy audit_log_stg \
    "SELECT id, event_name, event_description, user_id, event_initiator, event_status, created_at, updated_at FROM public.audit_log WHERE TRUE$(since_and created_at) ORDER BY id" \
    "id bigint, event_name varchar(255), event_description text, user_id bigint, event_initiator varchar(50), event_status varchar(50), created_at timestamptz, updated_at timestamptz"
  dst_exec "INSERT INTO audit_log (id, event_name, event_description, user_id, event_initiator, event_status, created_at, updated_at) SELECT id, event_name, event_description, user_id, event_initiator, event_status, created_at, updated_at FROM audit_log_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence audit_log id
  drop_staging audit_log_stg
  compare_counts audit_log \
    "SELECT count(*) FROM public.audit_log WHERE TRUE$(since_and created_at)" \
    "SELECT count(*) FROM audit_log WHERE TRUE$(since_and created_at)"
}

migrate_job_setting() {
  enum_check job_setting_job_name_precheck job_setting job_name "${JOB_NAMES[@]}"
  enum_check job_setting_initiator_precheck job_setting initiator "${JOB_INITIATORS[@]}"
  enum_check job_setting_execution_scope_precheck job_setting execution_scope "${JOB_SCOPES[@]}"
  enum_check job_setting_status_precheck job_setting status "${JOB_STATUSES[@]}"
  stage_copy job_setting_stg \
    "SELECT id, job_name, initiator, description, execution_scope, affected_records, started_at, finished_at, execution_time_ms, status, error_message, created_at, updated_at FROM public.job_setting WHERE TRUE$(since_and created_at) ORDER BY id" \
    "id bigint, job_name varchar(100), initiator varchar(50), description varchar(500), execution_scope varchar(100), affected_records bigint, started_at timestamptz, finished_at timestamptz, execution_time_ms bigint, status varchar(20), error_message text, created_at timestamptz, updated_at timestamptz"
  dst_exec "INSERT INTO job_setting (id, job_name, initiator, description, execution_scope, affected_records, started_at, finished_at, execution_time_ms, status, error_message, created_at, updated_at) SELECT id, job_name, initiator, description, execution_scope, affected_records, started_at, finished_at, execution_time_ms, status, error_message, created_at, updated_at FROM job_setting_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence job_setting id
  drop_staging job_setting_stg
  compare_counts job_setting \
    "SELECT count(*) FROM public.job_setting WHERE TRUE$(since_and created_at)" \
    "SELECT count(*) FROM job_setting WHERE TRUE$(since_and created_at)"
}

migrate_user_statistics() {
  enum_check user_statistics_event_type_precheck user_statistics event_type "${USER_EVENTS[@]}"
  assert_zero user_statistics_null_filter_duplicates_precheck "SELECT count(*) FROM (SELECT user_profile_id, COALESCE(user_filter_id, 0), event_type, event_date FROM public.user_statistics WHERE TRUE$(since_and created_at) GROUP BY 1, 2, 3, 4 HAVING count(*) > 1) d"
  stage_copy user_statistics_stg \
    "SELECT id, user_profile_id, user_filter_id, event_type, event_date, count, created_at, updated_at FROM public.user_statistics WHERE TRUE$(since_and created_at) ORDER BY id" \
    "id bigint, user_id bigint, filter_id bigint, event_type varchar(50), event_date date, count integer, created_at timestamptz, updated_at timestamptz"
  dst_exec "INSERT INTO user_statistics (id, user_id, filter_id, event_type, event_date, count, created_at, updated_at) SELECT id, user_id, filter_id, event_type, event_date, count, created_at, updated_at FROM user_statistics_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence user_statistics id
  drop_staging user_statistics_stg
  compare_counts user_statistics \
    "SELECT count(*) FROM public.user_statistics WHERE TRUE$(since_and created_at)" \
    "SELECT count(*) FROM user_statistics WHERE TRUE$(since_and created_at)"
}

main() {
  parse_args "$@"
  require_env MONOLITH_DB_URL TARGET_DB_URL
  validate_since
  if [[ -n ${MIGRATE_SINCE:-} ]]; then
    log "MIGRATE_SINCE=$MIGRATE_SINCE: audit_log, job_setting and user_statistics copy rows created since this moment"
  fi
  log "skipped (no monolith source): cleanup_saga, cleanup_saga_step, outbox_event, inbox_event"
  run_step audit_log migrate_audit_log
  run_step job_setting migrate_job_setting
  run_step user_statistics migrate_user_statistics
  log "migration finished"
}

main "$@"
