#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"

usage() {
  cat <<'USAGE'
Usage: verify.sh

Compares row counts between the monolith database (MONOLITH_DB_URL) and this
service's database (TARGET_DB_URL). Read-only. Exits non-zero on any MISMATCH.

Environment:
  MONOLITH_DB_URL   libpq URI of the monolith database
  TARGET_DB_URL     libpq URI of this service's database

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

verify_example_items() {
  compare_counts example_items \
    "SELECT count(*) FROM public.example_items" \
    "SELECT count(*) FROM example_items"
}

main() {
  parse_args "$@"
  require_env MONOLITH_DB_URL TARGET_DB_URL
  run_step example_items verify_example_items
  log "verification finished"
}

main "$@"
