#!/usr/bin/env bash
set -euo pipefail

echo "Waiting for cluster to be ready..."
kubectl wait --for=condition=Ready nodes --all --timeout=300s
kubectl wait --for=condition=Available deployment/studio-api --timeout=300s
echo "Cluster ready."
