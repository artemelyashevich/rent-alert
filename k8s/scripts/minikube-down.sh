#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K8S_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

overlay="minikube"
purge="false"

usage() {
    cat <<EOF
Usage: $(basename "$0") [minikube|minikube-ha] [--purge]

Deletes the rentalert resources of the given overlay (kubectl delete -k), which
removes the namespace and all data volumes in it. The minikube cluster itself
is kept unless --purge is given, which runs "minikube delete --purge".
EOF
}

for arg in "$@"; do
    case "$arg" in
        -h|--help) usage; exit 0 ;;
        minikube|minikube-ha) overlay="$arg" ;;
        --purge) purge="true" ;;
        *) echo "error: unknown argument '$arg'" >&2; usage >&2; exit 2 ;;
    esac
done

if ! command -v kubectl >/dev/null 2>&1; then
    echo "error: kubectl not found in PATH" >&2
    exit 1
fi

kubectl kustomize --load-restrictor=LoadRestrictionsNone "$K8S_DIR/overlays/$overlay" | kubectl delete -f - --ignore-not-found

if [[ "$purge" == "true" ]]; then
    if ! command -v minikube >/dev/null 2>&1; then
        echo "error: minikube not found in PATH" >&2
        exit 1
    fi
    minikube delete --purge
fi
