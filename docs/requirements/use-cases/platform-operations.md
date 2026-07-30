# Platform Operations Use Cases

| Use Case | Requirements | Preconditions | Successful Outcome |
|---|---|---|---|
| Audit consequential activity | FR-075 | A protected or state-changing operation occurs. | Tenant, actor, request, outcome, and timestamp metadata are recorded safely. |
| Build and verify release | NFR-016–NFR-018, NFR-027 | Lockfiles and test infrastructure are available. | Module, unit, DB, frontend, and E2E gates pass reproducibly. |
| Load-test release | NFR-007–NFR-010 | Representative dataset and environment are available. | Locust produces evidence against approved thresholds. |
| Observe runtime | NFR-022–NFR-026 | Application and telemetry pipeline are running. | Operators can correlate requests and detect health, DB, and outbox failures. |
| Restore platform | NFR-013, NFR-014 | CloudNativePG backup and WAL artifacts are available. | Restore meets approved RPO/RTO and evidence is retained. |

## Boundary Rules

- Terraform provisions infrastructure; Kustomize deploys EMME; Helm installs third-party components.
- k3d is the local runtime; Kubernetes with a single-node k3s VM is the deployment runtime.
- CI builds one backend image, not per-module images.
