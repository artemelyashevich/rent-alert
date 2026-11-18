#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DOCKERFILE="$REPO_ROOT/docker/Dockerfile.service"

declare -A PORTS=(
    [config-server]=8888
    [discovery]=8761
    [gateway]=8222
    [identity-ms]=8081
    [notification-ms]=8082
    [ops-ms]=8083
    [listing-ms]=8084
    [processing-ms]=8085
    [admin-ms]=8086
    [roommate-ms]=8087
    [realtime-ms]=8088
)
ALL_SERVICES=(config-server discovery gateway identity-ms listing-ms processing-ms notification-ms roommate-ms ops-ms admin-ms realtime-ms)

usage() {
    cat <<EOF
Usage: $(basename "$0") [--load] [SERVICE...]

Builds rentalert/<service>:local images from docker/Dockerfile.service with the
Docker daemon of the host. Without arguments builds all services.

  --load   after each build load the image into the minikube cluster
           (minikube image load); works with any minikube container runtime

Services: ${ALL_SERVICES[*]}
EOF
}

load=false
args=()
for arg in "$@"; do
    case "$arg" in
        -h|--help) usage; exit 0 ;;
        --load) load=true ;;
        *) args+=("$arg") ;;
    esac
done

if ! command -v docker >/dev/null 2>&1; then
    echo "error: docker not found in PATH" >&2
    exit 1
fi

if [[ "$load" == true ]] && ! command -v minikube >/dev/null 2>&1; then
    echo "error: minikube not found in PATH" >&2
    exit 1
fi

services=("${args[@]}")
if [[ ${#services[@]} -eq 0 ]]; then
    services=("${ALL_SERVICES[@]}")
fi

for svc in "${services[@]}"; do
    if [[ -z "${PORTS[$svc]:-}" ]]; then
        echo "error: unknown service '$svc'" >&2
        usage >&2
        exit 2
    fi
done

for svc in "${services[@]}"; do
    echo "==> building rentalert/$svc:local"
    DOCKER_BUILDKIT=1 docker build \
        --load \
        -f "$DOCKERFILE" \
        --build-arg SERVICE="$svc" \
        --build-arg PORT="${PORTS[$svc]}" \
        -t "rentalert/$svc:local" \
        "$REPO_ROOT"
    if [[ "$load" == true ]]; then
        echo "==> loading rentalert/$svc:local into minikube"
        minikube image load "rentalert/$svc:local"
    fi
done
