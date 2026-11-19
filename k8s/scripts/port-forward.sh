#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${NAMESPACE:-rentalert}"
GATEWAY_PORT="${GATEWAY_PORT:-8222}"
CONSOLE_PORT="${CONSOLE_PORT:-9999}"
POSTGRES_PORT="${POSTGRES_PORT:-15432}"
REDIS_PORT="${REDIS_PORT:-6379}"

usage() {
    cat <<EOF
Usage: $(basename "$0")

Forwards the gateway (http://localhost:\$GATEWAY_PORT), redpanda-console
(http://localhost:\$CONSOLE_PORT), postgres (localhost:\$POSTGRES_PORT) and redis
(localhost:\$REDIS_PORT) from the cluster to localhost. Stops all on Ctrl+C.

Environment:
  NAMESPACE     (default rentalert)
  GATEWAY_PORT  local port for the gateway (default 8222)
  CONSOLE_PORT  local port for redpanda-console (default 9999)
  POSTGRES_PORT local port for postgres (default 15432, 5432-5444 are often taken by local postgres containers)
  REDIS_PORT    local port for redis (default 6379)
EOF
}

case "${1:-}" in
    -h|--help) usage; exit 0 ;;
esac

if ! command -v kubectl >/dev/null 2>&1; then
    echo "error: kubectl not found in PATH" >&2
    exit 1
fi

trap 'kill $(jobs -p) 2>/dev/null || true' EXIT

kubectl -n "$NAMESPACE" port-forward svc/gateway "$GATEWAY_PORT:8222" &
kubectl -n "$NAMESPACE" port-forward svc/redpanda-console "$CONSOLE_PORT:9999" &
kubectl -n "$NAMESPACE" port-forward svc/postgres "$POSTGRES_PORT:5432" &
kubectl -n "$NAMESPACE" port-forward svc/redis "$REDIS_PORT:6379" &

echo "gateway:          http://localhost:$GATEWAY_PORT"
echo "redpanda console: http://localhost:$CONSOLE_PORT"
echo "postgres:         localhost:$POSTGRES_PORT"
echo "redis:            localhost:$REDIS_PORT"
wait
