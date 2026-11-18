#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib/common.sh"

usage() {
  cat <<'USAGE'
Usage: migrate-all.sh [options]

Runs <service>/migrate.sh for every service in dependency order:
identity-ms, listing-ms, processing-ms, notification-ms, roommate-ms, ops-ms.
Services without an executable migrate.sh are skipped.

Environment:
  MONOLITH_DB_URL           libpq URI of the monolith database (required)
  TARGET_DB_URL_<SERVICE>   libpq URI of a service database, e.g. TARGET_DB_URL_IDENTITY_MS
                            (required for each service that has a migrate.sh)

Options:
  --dry-run               pass --dry-run to every service script
  --only <service>        run only this service (repeatable)
  --skip <service>        do not run this service (repeatable)
  --continue-on-error     keep going after a failed service (default: stop at the first failure)
  -h, --help              show this help
USAGE
}

contains() {
  local needle=$1 item
  shift
  for item in "$@"; do
    [[ $item == "$needle" ]] && return 0
  done
  return 1
}

main() {
  local dry_run=0 continue_on_error=0 service script target_var failed=0
  local -a services=() only=() skip=() todo=() runnable=() args=()
  local -A status=()
  mapfile -t services < <(migration_services)

  while (( $# > 0 )); do
    case "$1" in
      --dry-run) dry_run=1 ;;
      --continue-on-error) continue_on_error=1 ;;
      --only|--skip)
        (( $# >= 2 )) || die "$1 requires a service name"
        contains "$2" "${services[@]}" || die "unknown service: $2"
        if [[ $1 == --only ]]; then only+=("$2"); else skip+=("$2"); fi
        shift
        ;;
      -h|--help) usage; exit 0 ;;
      *) usage >&2; die "unknown argument: $1" ;;
    esac
    shift
  done

  for service in "${services[@]}"; do
    if (( ${#only[@]} > 0 )) && ! contains "$service" "${only[@]}"; then
      status[$service]="SKIPPED (not in --only)"
    elif contains "$service" ${skip[@]+"${skip[@]}"}; then
      status[$service]="SKIPPED (--skip)"
    else
      todo+=("$service")
    fi
  done

  for service in ${todo[@]+"${todo[@]}"}; do
    script="$SCRIPT_DIR/$service/migrate.sh"
    if [[ -f $script && -x $script ]]; then
      runnable+=("$service")
    else
      log "$service: no executable migrate.sh, skipping"
      status[$service]="SKIPPED (no script)"
    fi
  done

  if (( ${#runnable[@]} > 0 )); then
    require_env MONOLITH_DB_URL
    for service in "${runnable[@]}"; do
      require_env "$(target_db_var "$service")"
    done
  fi

  if (( dry_run )); then
    args+=(--dry-run)
  fi

  for service in ${runnable[@]+"${runnable[@]}"}; do
    script="$SCRIPT_DIR/$service/migrate.sh"
    target_var="$(target_db_var "$service")"
    log "=== $service"
    if TARGET_DB_URL="${!target_var}" "$script" ${args[@]+"${args[@]}"}; then
      status[$service]="OK"
    else
      status[$service]="FAILED"
      failed=1
      if (( continue_on_error == 0 )); then
        log "stopping after failure in $service"
        break
      fi
    fi
  done

  log "=== summary"
  for service in "${services[@]}"; do
    printf '  %-16s %s\n' "$service" "${status[$service]:-NOT RUN}"
  done

  if (( failed != 0 )); then
    die "migration failed for one or more services"
  fi
}

main "$@"
