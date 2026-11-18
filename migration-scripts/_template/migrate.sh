#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"

usage() {
  cat <<'USAGE'
Usage: migrate.sh [--dry-run]

Copies data from the monolith database (MONOLITH_DB_URL) into this service's
database (TARGET_DB_URL). Steps are idempotent: re-running does not duplicate rows.

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

migrate_example_items() {
  stage_copy example_items_stg \
    "SELECT id, name, tags, note FROM public.example_items ORDER BY id" \
    "id bigint, name text, tags text[], note text"
  dst_exec "INSERT INTO example_items (id, name, tags, note) SELECT id, name, tags, note FROM example_items_stg ON CONFLICT (id) DO NOTHING"
  reset_sequence example_items id
  drop_staging example_items_stg
  compare_counts example_items \
    "SELECT count(*) FROM public.example_items" \
    "SELECT count(*) FROM example_items"
}

main() {
  parse_args "$@"
  require_env MONOLITH_DB_URL TARGET_DB_URL
  run_step example_items migrate_example_items
  log "migration finished"
}

main "$@"
