#!/usr/bin/env bash
set -euo pipefail

REGISTRY_NAME="${1:-emme-registry.local}"
REGISTRY_PORT="${2:-5000}"

echo "Creating local registry: ${REGISTRY_NAME}:${REGISTRY_PORT}"
k3d registry create "${REGISTRY_NAME}" --port "${REGISTRY_PORT}"
echo "Registry ready at localhost:${REGISTRY_PORT}"
