#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
NAMESPACE="${NAMESPACE:-rentalert}"
ROLLOUT_TIMEOUT="${ROLLOUT_TIMEOUT:-600s}"

FIRST_WAVE=(config-server discovery)
SECOND_WAVE=(identity-ms listing-ms processing-ms notification-ms roommate-ms ops-ms admin-ms realtime-ms)
LAST_WAVE=(gateway)

usage() {
    cat <<EOF
Usage: $(basename "$0") [--build] [--apply] [--overlay NAME] [SERVICE...]

Rolling restart of RentAlert microservices in the '$NAMESPACE' namespace.
Without SERVICE restarts all of them in dependency order:
  ${FIRST_WAVE[*]} -> ${SECOND_WAVE[*]} -> ${LAST_WAVE[*]}
Each wave must become ready before the next one is restarted.

  --build          rebuild images and load them into minikube first
  --apply          re-apply the kustomize overlay first (ConfigMap/Secret changes)
  --overlay NAME   overlay for --apply (default: minikube)

Env: NAMESPACE (default rentalert), ROLLOUT_TIMEOUT (default 600s)
EOF
}

build=false
apply=false
overlay=minikube
services=()
while [[ $# -gt 0 ]]; do
    case "$1" in
        -h|--help) usage; exit 0 ;;
        --build) build=true ;;
        --apply) apply=true ;;
        --overlay)
            [[ $# -ge 2 ]] || { echo "error: --overlay needs a value" >&2; exit 2; }
            overlay="$2"
            shift
            ;;
        -*) echo "error: unknown option '$1'" >&2; usage >&2; exit 2 ;;
        *) services+=("$1") ;;
    esac
    shift
done

if ! command -v kubectl >/dev/null 2>&1; then
    echo "error: kubectl not found in PATH" >&2
    exit 1
fi

ALL_SERVICES=("${FIRST_WAVE[@]}" "${SECOND_WAVE[@]}" "${LAST_WAVE[@]}")
for svc in "${services[@]}"; do
    known=false
    for candidate in "${ALL_SERVICES[@]}"; do
        [[ "$candidate" == "$svc" ]] && known=true
    done
    if [[ "$known" == false ]]; then
        echo "error: unknown service '$svc'" >&2
        usage >&2
        exit 2
    fi
done

if [[ "$build" == true ]]; then
    "$SCRIPT_DIR/build-images.sh" --load "${services[@]}"
fi

if [[ "$apply" == true ]]; then
    echo "==> applying overlay $overlay"
    kubectl kustomize --load-restrictor=LoadRestrictionsNone "$REPO_ROOT/k8s/overlays/$overlay" | kubectl apply -f -
fi

in_selection() {
    [[ ${#services[@]} -eq 0 ]] && return 0
    local candidate
    for candidate in "${services[@]}"; do
        [[ "$candidate" == "$1" ]] && return 0
    done
    return 1
}

restart_wave() {
    local wave=("$@") selected=() svc
    for svc in "${wave[@]}"; do
        in_selection "$svc" && selected+=("$svc")
    done
    [[ ${#selected[@]} -eq 0 ]] && return 0

    for svc in "${selected[@]}"; do
        echo "==> restarting $svc"
        kubectl -n "$NAMESPACE" rollout restart "deploy/$svc"
    done
    for svc in "${selected[@]}"; do
        kubectl -n "$NAMESPACE" rollout status "deploy/$svc" --timeout="$ROLLOUT_TIMEOUT"
    done
}

restart_wave "${FIRST_WAVE[@]}"
restart_wave "${SECOND_WAVE[@]}"
restart_wave "${LAST_WAVE[@]}"

echo "==> done"
kubectl -n "$NAMESPACE" get pods
