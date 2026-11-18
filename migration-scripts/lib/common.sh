#!/usr/bin/env bash
set -euo pipefail

log() {
  printf '[%s] %s\n' "$(date '+%H:%M:%S')" "$*"
}

die() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

is_dry_run() {
  [[ "${DRY_RUN:-0}" == "1" ]]
}

require_env() {
  local var
  for var in "$@"; do
    if [[ -z "${!var:-}" ]]; then
      die "environment variable $var is not set"
    fi
  done
}

src_psql() {
  psql -X -q -v ON_ERROR_STOP=1 "${MONOLITH_DB_URL:?MONOLITH_DB_URL is not set}" "$@"
}

dst_psql() {
  psql -X -q -v ON_ERROR_STOP=1 "${TARGET_DB_URL:?TARGET_DB_URL is not set}" "$@"
}

assert_identifier() {
  local what=$1 value=$2
  local LC_ALL=C
  [[ $value =~ ^[a-zA-Z_][a-zA-Z0-9_.]*$ ]] || die "invalid $what name: '$value'"
}

dst_exec() {
  if is_dry_run; then
    log "[dry-run] would execute on target: $1"
    return 0
  fi
  dst_psql -c "$1"
}

src_count_query() {
  src_psql -At -c "$1"
}

dst_count() {
  assert_identifier table "$1"
  dst_psql -At -c "SELECT count(*) FROM $1"
}

stage_copy() {
  local staging=$1 src_sql=$2 ddl=$3
  assert_identifier "staging table" "$staging"
  src_sql=${src_sql//$'\n'/ }
  if is_dry_run; then
    log "[dry-run] would stage $staging from monolith: $src_sql"
    return 0
  fi
  dst_exec "DROP TABLE IF EXISTS $staging; CREATE UNLOGGED TABLE $staging ($ddl)"
  if ! src_psql -c "\\copy ($src_sql) TO STDOUT WITH (FORMAT csv)" \
    | dst_psql -c "\\copy $staging FROM STDIN WITH (FORMAT csv)"; then
    die "copy into staging table $staging failed"
  fi
  log "staged $staging: $(dst_count "$staging") rows"
}

drop_staging() {
  assert_identifier "staging table" "$1"
  dst_exec "DROP TABLE IF EXISTS $1"
}

reset_sequence() {
  local table=$1 column=$2 seq max next
  assert_identifier table "$table"
  assert_identifier column "$column"
  seq=$(dst_psql -At -c "SELECT pg_get_serial_sequence('$table', '$column')")
  [[ -n $seq ]] || die "no sequence behind $table.$column"
  max=$(dst_psql -At -c "SELECT COALESCE(MAX(\"$column\"), 0) FROM $table")
  [[ $max =~ ^-?[0-9]+$ ]] || die "reset_sequence needs an integer column, $table.$column gave '$max'"
  next=$((max + 1))
  if is_dry_run; then
    log "[dry-run] would set $seq to next value $next"
    return 0
  fi
  dst_exec "SELECT setval('$seq', $next, false)"
  log "$seq: next value set to $next"
}

compare_counts() {
  local label=$1 src_sql=$2 dst_sql=$3 src dst
  src=$(src_count_query "$src_sql")
  dst=$(dst_psql -At -c "$dst_sql")
  if is_dry_run; then
    log "[$label] source=$src target=$dst DRY-RUN (not compared)"
    return 0
  fi
  if [[ $src == "$dst" ]]; then
    log "[$label] source=$src target=$dst OK"
  else
    log "[$label] source=$src target=$dst MISMATCH"
    return 1
  fi
}

run_step() {
  local label=$1 started=$SECONDS
  shift
  log "[$label] start"
  (
    trap 'rc=$?; if (( rc != 0 )); then log "[$label] FAILED (exit $rc)"; fi' EXIT
    set -e
    "$@"
  )
  log "[$label] OK ($((SECONDS - started))s)"
}

migration_services() {
  printf '%s\n' identity-ms listing-ms processing-ms notification-ms roommate-ms ops-ms
}

target_db_var() {
  printf 'TARGET_DB_URL_%s' "$(printf '%s' "$1" | tr '[:lower:]-' '[:upper:]_')"
}
