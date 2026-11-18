#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib/common.sh"

usage() {
  cat <<'USAGE'
Usage: verify-all.sh

Runs <service>/verify.sh for every service that has an executable verify.sh
and prints a summary. Services without verify.sh are reported as skipped.
All services are verified even if some fail; the exit code is non-zero if any failed.

Environment:
  MONOLITH_DB_URL           libpq URI of the monolith database (required)
  TARGET_DB_URL_<SERVICE>   libpq URI of a service database, e.g. TARGET_DB_URL_IDENTITY_MS
                            (required for each service that has a verify.sh)

Options:
  -h, --help                show this help
USAGE
}

main() {
  local service script target_var failed=0
  local -a services=() runnable=()
  local -A status=()
  mapfile -t services < <(migration_services)

  while (( $# > 0 )); do
    case "$1" in
      -h|--help) usage; exit 0 ;;
      *) usage >&2; die "unknown argument: $1" ;;
    esac
    shift
  done

  for service in "${services[@]}"; do
    script="$SCRIPT_DIR/$service/verify.sh"
    if [[ -f $script && -x $script ]]; then
      runnable+=("$service")
    else
      status[$service]="SKIPPED (no script)"
    fi
  done

  if (( ${#runnable[@]} > 0 )); then
    require_env MONOLITH_DB_URL
    for service in "${runnable[@]}"; do
      require_env "$(target_db_var "$service")"
    done
  fi

  for service in ${runnable[@]+"${runnable[@]}"}; do
    script="$SCRIPT_DIR/$service/verify.sh"
    target_var="$(target_db_var "$service")"
    log "=== $service"
    if TARGET_DB_URL="${!target_var}" "$script"; then
      status[$service]="OK"
    else
      status[$service]="FAILED"
      failed=1
    fi
  done

  log "=== summary"
  for service in "${services[@]}"; do
    printf '  %-16s %s\n' "$service" "${status[$service]:-NOT RUN}"
  done

  if (( failed != 0 )); then
    die "verification failed for one or more services"
  fi
}

main "$@"
