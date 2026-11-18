#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K8S_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
OVERLAYS_DIR="$K8S_DIR/overlays"
MINIKUBE_OVERLAY_DIR="$OVERLAYS_DIR/minikube"
SECRETS_FILE="$MINIKUBE_OVERLAY_DIR/secrets.env"
SECRETS_EXAMPLE="$MINIKUBE_OVERLAY_DIR/secrets.example.env"
NAMESPACE="${NAMESPACE:-rentalert}"
MINIKUBE_CPUS="${MINIKUBE_CPUS:-6}"
MINIKUBE_MEMORY="${MINIKUBE_MEMORY:-10240}"
MINIKUBE_DRIVER="${MINIKUBE_DRIVER:-docker}"
ROLLOUT_TIMEOUT="${ROLLOUT_TIMEOUT:-600s}"

REQUIRED_SECRETS=(
    POSTGRES_PASSWORD
    IDENTITY_DB_PASSWORD
    NOTIFICATION_DB_PASSWORD
    OPS_DB_PASSWORD
    LISTING_DB_PASSWORD
    PROCESSING_DB_PASSWORD
    ROOMMATE_DB_PASSWORD
    APP_AUTH_ACCESS_SECRET
    APP_AUTH_REFRESH_SECRET
    APP_INTERNAL_TOKEN
    APP_CRYPTO_AES_KEY
    APP_CRYPTO_HMAC_KEY
)

usage() {
    cat <<EOF
Usage: $(basename "$0") [minikube|minikube-ha]

Starts minikube, enables metrics-server, builds the service images with the host
Docker and loads them into the cluster, applies the kustomize overlay and waits for the rollout
(infra -> config-server -> discovery -> gateway -> services). Prints the gateway URL.

The first run creates k8s/overlays/minikube/secrets.env from secrets.example.env
and exits: fill in the required values and run the script again.

Environment:
  MINIKUBE_CPUS    CPU count for the cluster (default 6)
  MINIKUBE_MEMORY  memory in MB for the cluster (default 10240)
  MINIKUBE_DRIVER  minikube driver (default docker)
  ROLLOUT_TIMEOUT  per-workload rollout timeout (default 600s)
  NAMESPACE        namespace to wait for (default rentalert)

Required secrets: ${REQUIRED_SECRETS[*]}
Generate random values with: openssl rand -base64 32
EOF
}

case "${1:-minikube}" in
    -h|--help) usage; exit 0 ;;
    minikube|minikube-ha) overlay="${1:-minikube}" ;;
    *) echo "error: unknown overlay '${1}'" >&2; usage >&2; exit 2 ;;
esac

require() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "error: '$1' not found in PATH; $2" >&2
        exit 1
    fi
}

require docker "install Docker Engine: https://docs.docker.com/engine/install/"
require minikube "install minikube: https://minikube.sigs.k8s.io/docs/start/"
require kubectl "install kubectl: https://kubernetes.io/docs/tasks/tools/"

ensure_secrets() {
    if [[ ! -f "$SECRETS_FILE" ]]; then
        cp "$SECRETS_EXAMPLE" "$SECRETS_FILE"
        chmod 600 "$SECRETS_FILE"
        echo "warning: created $SECRETS_FILE from the example; fill in the values and run again" >&2
        exit 1
    fi

    local missing=()
    local key value
    for key in "${REQUIRED_SECRETS[@]}"; do
        value="$(sed -n "s/^${key}=//p" "$SECRETS_FILE" | tail -n 1)"
        if [[ -z "${value//[[:space:]]/}" ]]; then
            missing+=("$key")
        fi
    done

    if [[ ${#missing[@]} -gt 0 ]]; then
        echo "error: empty values in $SECRETS_FILE: ${missing[*]}" >&2
        exit 1
    fi
}

ensure_secrets

minikube start --cpus "$MINIKUBE_CPUS" --memory "$MINIKUBE_MEMORY" --driver "$MINIKUBE_DRIVER"
minikube addons enable metrics-server

"$SCRIPT_DIR/build-images.sh" --load

kubectl kustomize --load-restrictor=LoadRestrictionsNone "$OVERLAYS_DIR/$overlay" | kubectl apply -f -

wait_for() {
    kubectl -n "$NAMESPACE" rollout status "$1/$2" --timeout="$ROLLOUT_TIMEOUT"
}

wait_for statefulset postgres
wait_for deployment redis
wait_for statefulset kafka
wait_for deployment redpanda-console
wait_for deployment config-server
wait_for deployment discovery
wait_for deployment gateway

for svc in identity-ms listing-ms processing-ms notification-ms roommate-ms ops-ms admin-ms realtime-ms; do
    wait_for deployment "$svc"
done

echo "gateway URL:"
minikube service gateway -n "$NAMESPACE" --url
