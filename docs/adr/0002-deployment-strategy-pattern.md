# ADR-0002: Strategy Pattern for Deployment

## Status
Accepted (2026-07-10)

## Context
Emme needs to deploy to multiple environments: local development (Docker Compose), CI testing (k3d), staging, and production (Kubernetes). Each target has different commands, configurations, and lifecycle. A monolithic plugin that knows about all targets becomes unmaintainable as targets grow.

## Decision
Use a strategy pattern. `EmmeDeploymentPlugin` reads a single `target` property (gradle property or env variable), then dispatches to a self-contained target class.

### Targets
- `ComposeTarget` — docker compose up/down/logs
- `K3dTarget` — k3d cluster create/import/delete
- `KubernetesTarget` — kubectl apply/diff/rollout/status/logs

### Target selection
```bash
-Pemme.deployment.target=compose     # gradle property
EMME_DEPLOYMENT_TARGET=k3d           # environment variable
```

## Consequences
- **Positive**: Adding a new target (e.g., k3s, Nomad) requires 1 new file, zero changes to existing targets.
- **Positive**: Each target file is 40-90 lines, easy to understand and test.
- **Negative**: `afterEvaluate` required for target dispatch (target property not available during configuration phase without workaround).
