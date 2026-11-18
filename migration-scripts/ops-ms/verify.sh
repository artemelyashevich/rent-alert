#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"

usage() {
  cat <<'USAGE'
Usage: verify.sh

Compares row counts and control aggregates between the monolith database (MONOLITH_DB_URL)
and the ops-ms database (TARGET_DB_URL). Read-only. Exits non-zero on any MISMATCH.

Environment:
  MONOLITH_DB_URL   libpq URI of the monolith database
  TARGET_DB_URL     libpq URI of the ops-ms database
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

verify_audit_log() {
  compare_counts audit_log \
    "SELECT count(*) FROM public.audit_log WHERE TRUE$(since_and created_at)" \
    "SELECT count(*) FROM audit_log WHERE TRUE$(since_and created_at)"
  compare_aggregate audit_log_by_initiator_status \
    "SELECT event_initiator || '/' || event_status || '=' || count(*) FROM public.audit_log WHERE TRUE$(since_and created_at) GROUP BY event_initiator, event_status ORDER BY 1" \
    "SELECT event_initiator || '/' || event_status || '=' || count(*) FROM audit_log WHERE TRUE$(since_and created_at) GROUP BY event_initiator, event_status ORDER BY 1"
}

verify_job_setting() {
  compare_counts job_setting \
    "SELECT count(*) FROM public.job_setting WHERE TRUE$(since_and created_at)" \
    "SELECT count(*) FROM job_setting WHERE TRUE$(since_and created_at)"
  compare_aggregate job_setting_by_job_status \
    "SELECT job_name || '/' || status || '=' || count(*) FROM public.job_setting WHERE TRUE$(since_and created_at) GROUP BY job_name, status ORDER BY 1" \
    "SELECT job_name || '/' || status || '=' || count(*) FROM job_setting WHERE TRUE$(since_and created_at) GROUP BY job_name, status ORDER BY 1"
}

verify_user_statistics() {
  compare_counts user_statistics \
    "SELECT count(*) FROM public.user_statistics WHERE TRUE$(since_and created_at)" \
    "SELECT count(*) FROM user_statistics WHERE TRUE$(since_and created_at)"
  compare_aggregate user_statistics_events_by_type \
    "SELECT event_type || '=' || count(*) || '/' || sum(count) FROM public.user_statistics WHERE TRUE$(since_and created_at) GROUP BY event_type ORDER BY 1" \
    "SELECT event_type || '=' || count(*) || '/' || sum(count) FROM user_statistics WHERE TRUE$(since_and created_at) GROUP BY event_type ORDER BY 1"
}

main() {
  parse_args "$@"
  require_env MONOLITH_DB_URL TARGET_DB_URL
  validate_since
  run_step audit_log verify_audit_log
  run_step job_setting verify_job_setting
  run_step user_statistics verify_user_statistics
  log "verification finished"
}

main "$@"
