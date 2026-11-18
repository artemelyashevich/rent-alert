#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../lib/common.sh"

usage() {
  cat <<'USAGE'
Usage: verify.sh

Compares row counts of the identity domain tables and a few business aggregates between the
monolith database (MONOLITH_DB_URL) and the identity-ms database (TARGET_DB_URL). Read-only.
Exits non-zero on any MISMATCH.

Environment:
  MONOLITH_DB_URL   libpq URI of the monolith database
  TARGET_DB_URL     libpq URI of the identity-ms database

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

verify_user_profiles() {
  compare_counts user_profiles \
    "SELECT count(*) FROM public.user_profiles" \
    "SELECT count(*) FROM user_profiles"
}

verify_user_settings() {
  compare_counts user_settings \
    "SELECT count(*) FROM public.user_settings" \
    "SELECT count(*) FROM user_settings"
}

verify_user_filters() {
  compare_counts user_filters \
    "SELECT count(*) FROM public.user_filters" \
    "SELECT count(*) FROM user_filters"
}

verify_feedbacks() {
  compare_counts feedbacks \
    "SELECT count(*) FROM public.feedbacks" \
    "SELECT count(*) FROM feedbacks"
}

verify_aggregates() {
  compare_counts active_filters \
    "SELECT count(*) FROM public.user_filters WHERE is_active" \
    "SELECT count(*) FROM user_filters WHERE is_active"
  compare_counts owner_match_filters \
    "SELECT count(*) FROM public.user_filters WHERE owner_match_id IS NOT NULL" \
    "SELECT count(*) FROM user_filters WHERE owner_match_id IS NOT NULL"
  compare_counts confirmed_email_users \
    "SELECT count(*) FROM public.user_settings WHERE is_email_active" \
    "SELECT count(*) FROM user_settings WHERE is_email_active"
}

main() {
  parse_args "$@"
  require_env MONOLITH_DB_URL TARGET_DB_URL
  run_step user_profiles verify_user_profiles
  run_step user_settings verify_user_settings
  run_step user_filters verify_user_filters
  run_step feedbacks verify_feedbacks
  run_step aggregates verify_aggregates
  log "verification finished"
}

main "$@"
