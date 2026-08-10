# EMME Service Environment Deployment Design

| Field | Detail |
|---|---|
| Date | 2026-08-05 |
| Status | Approved for specification review |
| Owning repository | `emme-service` |
| Related repository | `emme-web` owns the Vite application and frontend Nginx image source |
| Scope | Environment naming, Compose, Kustomize, Kubernetes Secrets, frontend-to-backend proxying, and release validation |

## 1. Summary

`emme-service` is the deployment authority for the backend, frontend image wiring, Compose environments, Kubernetes/Kustomize overlays, secrets delivery, ingress, and release promotion. `emme-web` remains the source owner for Vite and the frontend Nginx template because the frontend source is not present in this repository.

Compose is limited to `local` and `regression`. Kubernetes with Kustomize is authoritative for `dev`, `staging`, and `prod`. Browser traffic uses the frontend origin: Vite proxies API traffic during development, and the frontend Nginx runtime proxies the same paths to the Kubernetes `backend:8081` service. The browser never needs a separate cross-origin API URL in deployed environments.

## 2. Repository Ownership

| Concern | Owner | Contract |
|---|---|---|
| Backend application and API | `emme-service` | `backend` Kubernetes Service on port `8081` |
| Vite development proxy | `emme-web` | `API_PROXY_TARGET`, same-origin path forwarding |
| Frontend production Nginx template | `emme-web` | `EMME_API_UPSTREAM`, same-origin path forwarding |
| Frontend image deployment | `emme-service` | `frontend` Deployment uses `ghcr.io/migangdelzar/emme-web` |
| Environment overlays | `emme-service` | Kustomize and Compose names use the canonical environment vocabulary |
| Runtime secrets | `emme-service` | Kubernetes Secret objects are the runtime boundary |
| Secret source | Bitwarden or GitHub Actions | Explicit provider selected by deployment automation |

The frontend source and its Vite/Nginx files are not duplicated into `emme-service`. Deployment changes in `emme-service` must configure and verify the frontend image contract instead.

## 3. Environment Matrix

| Environment | Runtime | Canonical target | Namespace | Role |
|---|---|---|---|---|
| `local` | Docker Compose | `compose` | N/A | Individual development |
| `dev` | Kubernetes/K3d | `k3d` | `emme-dev` | Shared integration and local Kubernetes validation |
| `regression` | Docker Compose | `compose` | N/A | Deterministic automated validation |
| `staging` | Kubernetes | `k3s`/configured cluster | `emme-staging` | Production-like candidate |
| `prod` | Kubernetes | `k3s`/configured cluster | `emme-prod` | Protected production |

The environment is selected explicitly through Gradle `-Penvironment=<name>` or `EMME_ENV=<name>`. It is never inferred from an image tag, namespace, or hostname. `production` remains only as a migration alias until all consumers use `prod`; new files and commands use `prod`.

## 4. Canonical File and Overlay Naming

### Compose

The generic base name is made explicit and environment overlays are named directly:

```text
deployment/compose/
├── compose.base.yaml
├── compose.local.yaml
├── compose.regression.yaml
├── compose.kafka.yaml
├── compose.observability.yaml
├── compose.runtime-jvm.yaml
├── compose.runtime-native.yaml
└── env/
    ├── local.env.example
    └── regression.env.example
```

`compose.environment-local.yaml`, `compose.environment-regression.yaml`, and `compose.environment-ci.yaml` are migrated to the direct names. The CI environment is not a deployment environment; regression is the canonical automated environment.

The base file owns shared backend, PostgreSQL/pgvector, Redis, network, and volume definitions. Exactly one runtime overlay and exactly one environment overlay are selected. The Compose project name is `emme-<environment>`.

### Kubernetes/Kustomize

The runtime overlay naming matches the Gradle deployment provider:

```text
infra/kubernetes/
├── base/
├── jobs/
└── overlays/
    ├── dev-jvm/
    ├── dev-native/
    ├── staging-jvm/
    ├── staging-native/
    ├── prod-jvm/
    └── prod-native/
```

The existing `k3d-*` overlays become `dev-*`; `k3s-production-*` becomes `prod-*`; staging receives first-class overlays. Runtime selection remains independent from environment selection: JVM and native images are never applied together.

Kustomize base resources remain technology-neutral. Overlays own namespace, image digest, replica count, resource budgets, hostname/TLS, environment profile, secret references, network policy, and public service exposure.

Helm remains limited to third-party or explicitly chart-managed installations. It is not a second source of truth for the application environment overlays.

## 5. Frontend-to-Backend Routing Contract

The public entry point is the frontend. The backend Service is cluster-internal and is not directly exposed to browsers.

```text
Browser
  │ same-origin /api, /oauth2, /login/oauth2, /q
  ▼
Ingress → frontend:80
              │ Nginx proxy
              ▼
          backend:8081
```

The frontend Nginx container receives this Kubernetes environment value in every Kubernetes overlay:

```yaml
env:
  - name: EMME_API_UPSTREAM
    value: backend:8081
```

This is required because the Nginx image default is intended for Docker-host local use and points to `host.docker.internal:8081`. The Kubernetes service DNS name is the only supported production-like upstream.

The proxy path contract includes:

- `/api/**`
- `/oauth2/**`
- `/login/oauth2/**`
- `/q/**`

The Nginx proxy preserves `Host`, forwarding headers, authorization, cookies, and connection semantics. Response buffering is disabled and read/send timeouts are extended for dashboard Server-Sent Events. SPA fallback is applied only to non-API paths.

Vite development uses `API_PROXY_TARGET`, defaulting to the local backend address. Public browser configuration uses same-origin API paths; private credentials never use `VITE_*` variables.

The Kubernetes ingress routes the configured environment hostname to `frontend:80`. It does not expose a separate browser-facing backend route. Backend health and operational endpoints remain cluster/service or controlled operator access paths.

## 6. Kubernetes Secrets and Configuration

Non-sensitive environment configuration is represented through Kustomize `ConfigMap`s or overlay values. Sensitive values are represented only by namespaced Kubernetes `Secret` objects at runtime.

The backend base deployment contains no plaintext secret values, placeholder credentials, or empty secret environment values. Secret references use stable keys and names such as:

- `emme-runtime-secrets`
- `emme-database-credentials`
- `emme-identity-credentials`
- `emme-messaging-credentials`

The exact key contract is validated before apply and includes database credentials, OAuth/identity credentials, encryption keys, messaging credentials when enabled, and external integration tokens required by the selected profile.

Secret source selection is explicit:

```text
SECRETS_PROVIDER=bitwarden|github-actions
```

- Bitwarden reads the selected environment project and materializes the required Kubernetes Secrets.
- GitHub Actions reads protected environment secrets and materializes the same Kubernetes Secret contract.
- There is no silent fallback between providers.
- Secret values are never committed, rendered into source-controlled manifests, printed, or included in deployment evidence.
- Secret rotation updates the Kubernetes Secret and causes a controlled frontend/backend rollout where the application reads values at startup.
- Local and regression Compose use ignored files or CI-injected variables and fail when required values are absent; insecure defaults are removed.

## 7. Build, Promotion, and Rollback

```text
source commit
   ↓
build backend + frontend artifacts
   ↓
regression Compose validation
   ↓ promote immutable backend/frontend digests
staging Kustomize rollout + smoke tests
   ↓ approval
prod Kustomize rollout + health/telemetry verification
```

The backend and frontend image digests validated in regression are promoted without rebuilding for staging or production. Mutable `latest` references are prohibited for staging and production. Each deployment records environment, commit, image digests, overlay, cluster context, secret provider, rollout result, and health evidence without recording secret material.

Production requires an explicit environment, protected authorization, valid digests, matching overlay and namespace, a successful secret contract check, and a rollback digest. Rollback restores the previous known-good pair of backend and frontend digests and re-runs readiness and smoke checks.

## 8. Fifteen-Factor Release Gates

The release checklist verifies these project-specific factors:

1. **Codebase** — source commit and image provenance are traceable.
2. **Dependencies** — Gradle/Bun lockfiles and container bases are reproducible.
3. **Configuration** — non-secret configuration is externalized per environment.
4. **Backing services** — PostgreSQL, Redis, identity, messaging, and AI services are explicit resources.
5. **Build/release/run** — the validated image digest is separated from environment release.
6. **Processes** — backend and frontend processes are isolated and stateless where required.
7. **Port binding** — frontend, backend, health, and ingress ports are explicit.
8. **Concurrency** — replicas, HPA, and resource budgets are environment-specific.
9. **Disposability** — startup, readiness, liveness, termination, and rollout behavior are tested.
10. **Parity** — Compose and Kubernetes share service names, API paths, and secret contracts.
11. **Logs** — workloads emit logs to standard output/error for collection.
12. **Administrative processes** — migrations and provisioning run through explicit controlled jobs.
13. **API contract** — the browser uses same-origin API paths and the backend version contract.
14. **Telemetry** — health, metrics, alerts, rollout, and deployment evidence are available.
15. **Automation/security** — promotion, secret handling, least privilege, network policy, immutable images, and rollback are enforced.

## 9. Validation Strategy

- Compose base, runtime, local, and regression layers pass `docker compose config`.
- Kustomize renders all six runtime overlays without unresolved references.
- Kubernetes dry-run/schema validation passes for each overlay.
- Every frontend overlay sets `EMME_API_UPSTREAM=backend:8081`.
- Backend credentials are loaded only through `secretKeyRef`/equivalent Secret references.
- Backend is not exposed as a public browser route; frontend is the public service.
- Ingress hosts and TLS settings match the selected environment.
- Frontend Nginx and Vite proxy tests pass in `emme-web`.
- Regression smoke tests exercise authentication, API calls, OAuth callbacks, and SSE through the frontend origin.
- Staging verifies ingress/TLS, rollout, telemetry, and rollback.
- Production verification is protected and records health/routing evidence.
- Existing service tests, architecture checks, formatting, and deployment validation remain green.

## 10. Migration Sequence

1. Add the service-specific canonical environment and ownership documentation.
2. Rename Compose files and update the Compose deployment provider.
3. Normalize environment properties and replace `production` with `prod`.
4. Rename Kustomize overlays and add staging.
5. Add frontend upstream configuration, ingress, and remove public backend exposure.
6. Replace plaintext/placeholder backend credentials with Kubernetes Secret references.
7. Add Bitwarden/GitHub Actions provider validation and Secret materialization.
8. Pin backend/frontend release images by digest and add promotion/rollback checks.
9. Align `emme-web` Vite/Nginx tests and deployment documentation with the service contract.
10. Render, dry-run, smoke-test, and document all environment targets.

The migration preserves existing user worktree changes and keeps each rename or routing change independently reversible until its validation gate passes.

