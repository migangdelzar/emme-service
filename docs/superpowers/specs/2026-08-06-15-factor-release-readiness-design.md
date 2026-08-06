# 16-Factor Release Readiness Design

| Field | Detail |
|---|---|
| Date | 2026-08-06 |
| Status | Approved for implementation planning |
| Owning repository | `emme-service` |
| Related repository | `emme-web` |
| Scope | Java 25, GraalVM 25, Compose, Kustomize, Kubernetes Secrets, frontend proxying, CI/CD, observability, rollback, self-managed k3s deployment, and release evidence |

## 1. Objective

Complete the release path for the EMME platform by converting the existing release checklist into executable repository changes and evidence gates. The service repository owns backend delivery and environment orchestration. The web repository owns the Vite application and frontend Nginx image source.

The release model keeps Java 25 Temurin as the JVM baseline and adds an explicit GraalVM 25 Native Image track. JVM and native artifacts are built independently, scanned, smoke-tested, and promoted by immutable digest. The JVM artifact remains the immediate rollback path until native evidence is accepted.

This project-specific checklist is named **EMME 16-Factor Release Controls**. It is a release-readiness control model, not a claim that there is one universal sixteen-factor standard.

## 2. Repository Ownership

| Concern | Repository | Source of truth |
|---|---|---|
| Backend Java 25 application | `emme-service` | Gradle conventions and application build |
| GraalVM Native Image | `emme-service` | `emme.native-image` capability and native workflow |
| Vite development proxy | `emme-web` | `apps/emme-salon-app/vite.config.ts` |
| Frontend Nginx runtime proxy | `emme-web` | `apps/emme-salon-app/nginx.conf.template` and Dockerfile |
| Backend/frontend image deployment | `emme-service` | `infra/kubernetes` and CI release workflow |
| Compose environments | `emme-service` | `deployment/compose` |
| Kubernetes environments | `emme-service` | `infra/kubernetes` Kustomize overlays |
| Self-managed Kubernetes infrastructure | `emme-service` | `infra/terraform`, `infra/k3s`, and deployment runbooks |
| Secret materialization | `emme-service` | Bitwarden/GitHub Actions provider and Kubernetes Secret contract |

Frontend source and proxy configuration are not duplicated into `emme-service`. The service repository configures the deployed web image with the Kubernetes service upstream and verifies the cross-repository contract.

## 3. Environment and Runtime Matrix

| Environment | Runtime | Overlay/profile | Namespace/project | Artifact policy |
|---|---|---|---|---|
| `local` | Docker Compose JVM by default; optional K3d JVM/native parity lane | `compose.local` / `local-k3d` (`infra/kubernetes/local`) | `emme-local` Compose project or local K3d cluster | Local build/tag |
| `dev` | Single-node self-managed k3s JVM or native | `dev-jvm` / `dev-native` | `emme-dev` | Development tag or digest |
| `regression` | Docker Compose JVM; optional native lane | `compose.regression` | `emme-regression` Compose project | Candidate digest |
| `staging` | Single-node self-managed k3s JVM or native | `staging-jvm` / `staging-native` | `emme-staging` | Promoted immutable digest |
| `prod` | HA self-managed k3s JVM or native | `prod-jvm` / `prod-native` | `emme-prod` | Approved immutable digest |

Environment selection is explicit through `EMME_ENV` or `-Penvironment`. The unreleased service uses only the canonical identifiers `local`, `dev`, `regression`, `staging`, and `prod`; obsolete names such as `production`, `test`, `ci`, `k3d-*`, and `k3s-production-*` are removed rather than retained as indefinite aliases.

The canonical environment contract is:

```text
EnvironmentName
  → gradle/environments/<environment>.properties
  → deployment overlay
  → namespace / Compose project
  → Secret project and Kubernetes Secret keys
  → image promotion metadata
  → CI environment
  → public hostname
```

The environment identifier is passed unchanged through each boundary. Runtime is a separate value (`jvm` or `native`) and the overlay name is derived from environment plus runtime; it is not independently configured in a second source of truth.

## 4. Release Flow

```text
source commit
  ↓
code/dependency/config/security checks
  ↓
Java 25 JVM build + frontend build
  ↓
optional GraalVM 25 native build
  ↓
immutable images + SBOM + scan + provenance
  ↓
Compose regression and frontend-origin smoke tests
  ↓ promote the same digests
staging Kustomize render → Secret validation → rollout → smoke/telemetry
  ↓ protected approval
production Kustomize render → Secret validation → rollout → health evidence
  ↓
release record and tested rollback reference
```

No staging or production image is rebuilt. A release promotion must reference the exact backend and frontend digests validated by regression.

Each release produces a backend/frontend release bundle before promotion:

```yaml
release: 2026.08.06-rc.1
apiContract: 1.0
backend:
  image: ghcr.io/migangdelzar/emme-service
  digest: sha256:<backend-digest>
  sourceSha: <service-sha>
frontend:
  image: ghcr.io/migangdelzar/emme-web
  digest: sha256:<frontend-digest>
  sourceSha: <web-sha>
```

Staging, production, and rollback consume this pair rather than independently selecting tags.

## 4.1 Standardized Runtime and Secret Contracts

The canonical application service name is `backend` in both Compose and Kubernetes. The image remains `ghcr.io/migangdelzar/emme-service`. The frontend image remains `ghcr.io/migangdelzar/emme-web` and receives `EMME_API_UPSTREAM=backend:8081` in Kubernetes.

Runtime secrets use these namespaced Kubernetes Secret objects:

| Secret | Required key groups |
|---|---|
| `emme-runtime-secrets` | application encryption/signing keys and external integration tokens |
| `emme-database-credentials` | database username, password, and connection credentials |
| `emme-identity-credentials` | OAuth/Keycloak client and administrator credentials |
| `emme-messaging-credentials` | Kafka credentials when messaging is enabled |

Bitwarden and GitHub Actions are source providers. A provider-specific resolver validates required keys, then materializes the same Kubernetes Secret contract with `kubectl` before workload apply. No provider silently falls back to another provider. Secret values are never committed, rendered into source-controlled files, or logged.

Java build execution uses Temurin 25. Native compilation uses an explicit GraalVM Community 25 installation supplied through `GRAALVM_HOME`/`JAVA_HOME`; Native Build Tools automatic vendor detection is not the release contract. Native fallback remains disabled.

## 4.2 Gradle, mise, and configuration standard

Gradle is the canonical implementation and CI API for backend behavior. `mise` is the stable developer-facing and operator-facing command API. A mise task may select tools, normalize environment variables, and delegate to Gradle, Compose, Kustomize, or a release script, but it must not contain a second implementation of backend build logic.

Gradle task identifiers use conventional camelCase names and are grouped by their `group` metadata. They must remain stable across environments:

| Gradle group | Canonical tasks | Contract |
|---|---|---|
| `environment` | `verifyEnvironment`, `environmentReport` | Validate and report the canonical environment contract. |
| `quality` | `spotlessCheck`, `spotlessApply`, `architectureTest`, `coverageCheck`, `securityScan`, `apiCheck` | Formatting, architecture, coverage, dependency/security, and API gates. |
| `build` | `compileJava`, `test`, `integrationTest`, `e2eTest`, `build`, `ci`, `full` | JVM compilation, tests, packaging, and aggregate gates. |
| `native` | `nativeCompile`, `nativeTest` | Explicit GraalVM 25 Native Image lane; no JVM fallback. |
| `container` | `containerBuild`, `containerPush`, `containerVerify`, `containerMultiArch` | Image construction, publication, verification, and multi-architecture output. |
| `release` | `publishBuildInfo`, `publishManifest`, `publishVerifyVersion`, `publishSign`, `publishSbom` | Release metadata, signing, version, and SBOM artifacts. |
| `deploy` | `deployUp`, `deployDown`, `deployApply`, `deployStatus`, `deployLogs` | Environment-aware deployment operations. |

Existing task implementations are renamed or grouped to satisfy this table; new lifecycle tasks must compose existing tasks instead of duplicating their shell commands. Gradle task names must not encode an infrastructure vendor (`k3d`, `k3s`) or an obsolete environment (`production`).

Mise exposes namespaced commands whose names describe intent and target. The public task contract is:

| Mise namespace | Examples | Required inputs |
|---|---|---|
| `toolchain:*` | `toolchain:jvm`, `toolchain:native` | Toolchain lane only. |
| `env:*` | `env:verify`, `env:report` | `EMME_ENV`. |
| `docs:*` | `docs:check` | None beyond the checkout. |
| `quality:*` | `quality:format:check`, `quality:format:apply`, `quality:architecture`, `quality:coverage`, `quality:security`, `quality:all` | `EMME_ENV` only when a gate is environment-aware. |
| `build:*` | `build:compile`, `build:test`, `build:integration`, `build:e2e`, `build:package`, `build:native` | `EMME_RUNTIME` where applicable. |
| `container:*` | `container:build`, `container:verify`, `container:push`, `container:multi-arch` | Immutable image references and release metadata. |
| `release:*` | `release:validate`, `release:manifest`, `release:sbom`, `release:sign`, `release:promote`, `release:rollback` | Release bundle and explicit target. |
| `compose:*` | `compose:config`, `compose:up`, `compose:down`, `compose:logs` | `EMME_ENV=local|regression`, optional `EMME_RUNTIME`. |
| `kubernetes:*` | `kubernetes:bootstrap`, `kubernetes:render`, `kubernetes:apply`, `kubernetes:status`, `kubernetes:logs`, `kubernetes:rollback` | `EMME_ENV=local|dev|staging|prod`, `EMME_RUNTIME`, and cluster context. |
| `e2e:*` | `e2e:provision`, `e2e:clean`, `e2e:reset`, `e2e:smoke` | Explicit environment and test data scope. |

The old top-level mise names (`compile`, `test`, `quality`, `format-check`, `format-apply`, `architecture`, `arch-test`, `coverage`, `security-check`, and `build`) are migrated to the namespaced contract. Infrastructure-specific names (`k3d:*`, `k3s:*`) become `kubernetes:*` with `EMME_ENV` selecting the environment. Ambiguous `platform:*` tasks become `compose:*`, `container:*`, or `build:*` according to their actual intent. Because this service is unreleased, obsolete names are removed after all CI, documentation, and developer scripts are migrated; compatibility aliases are not retained indefinitely.

The shared configuration contract is:

```text
EMME_ENV=local|dev|regression|staging|prod
EMME_RUNTIME=jvm|native
EMME_DEPLOYMENT_TARGET=compose|kubernetes
EMME_SECRETS_PROVIDER=bitwarden|github-actions|environment
EMME_BACKEND_IMAGE=<immutable image reference>
EMME_FRONTEND_IMAGE=<immutable image reference>
EMME_RELEASE_BUNDLE=<release bundle path or identifier>
```

The corresponding Gradle properties are `-Penvironment`, `-Pdeployment.runtime`, `-Pdeployment.target`, `-Psecrets.provider`, `-Pbackend.image`, `-Pfrontend.image`, and `-Prelease.bundle`. Mise tasks translate environment variables to these properties in one shared helper; CI invokes the same mise commands or the same Gradle tasks with the same values. `EMME_SECRETS_PROVIDER=environment` is permitted only for `local` and `regression` and must never be accepted by staging or production tasks.

Configuration ownership is deliberately non-overlapping:

| Layer | Owns | Must not own |
|---|---|---|
| `mise.toml` | Tool versions, task names, delegation, safe defaults | Application settings, credentials, duplicate Gradle logic |
| `gradle/environments/<env>.properties` | Non-secret backend/build settings for one canonical environment | Secret values, overlay names, image promotion state |
| `deployment/compose/*.yaml` and `env/*.env.example` | Disposable local/regression service wiring and examples | Staging/prod credentials or Kubernetes-only routing |
| `infra/kubernetes` Kustomize overlays | Environment/runtime patches, namespaces, image digests, resource policy | Provider-specific secret values or duplicate base manifests |
| Secret provider materialization | Runtime secret values | Non-secret configuration and source-controlled manifests |
| Release bundle | Paired image digests, source SHAs, API contract, promotion metadata | Mutable tags or secret values |

Every task that changes state must declare its environment and runtime, print the resolved non-secret target, support a dry-run/render mode where applicable, and provide a corresponding status or rollback command. Every task used by CI must be non-interactive and return a non-zero exit code on contract violations.

## 5. Point-by-Point Implementation and Verification Plan

### Factor 1 — Codebase and provenance

Changes:

- Add release metadata to backend and frontend image labels: source repository, commit SHA, build timestamp, and release identifier.
- Add CI checks for untracked files, accidental secret files, and dirty generated manifests.
- Record the two repository SHAs used for every cross-repository release.

Primary files:

- `emme-service/.github/workflows/container-image.yml`
- `emme-service` container convention and release metadata configuration
- `emme-web/apps/emme-salon-app/Dockerfile`

Evidence:

- Clean CI checkout.
- Image labels match the release commit.
- Deployment evidence records backend/frontend SHAs.

### Factor 2 — Dependencies

Changes:

- Keep Gradle dependency locking and dependency verification mandatory.
- Verify all Gradle plugins and Native Build Tools artifacts are present in verification metadata.
- Require `bun install --frozen-lockfile` for the web build.
- Pin runtime and build container images by version or digest.
- Reject dynamic or mutable production dependency references.

Primary files:

- `gradle/libs.versions.toml`
- `gradle/verification-metadata.xml`
- `gradle.lockfile` and settings lockfiles
- `emme-web/package.json`, `bun.lock`
- deployment image definitions

Evidence:

- Locked backend/frontend installs.
- Dependency vulnerability report.
- No unpinned production image or dependency.

### Factor 3 — Configuration

Changes:

- Normalize `gradle/environments` to `local`, `dev`, `regression`, `staging`, and `prod`.
- Rename `production.properties` to `prod.properties` after updating consumers.
- Rename Compose overlays to `compose.base.yaml`, `compose.local.yaml`, and `compose.regression.yaml`.
- Rename Kubernetes overlays to `dev-*`, `staging-*`, and `prod-*`.
- Keep non-secret values in environment properties, ConfigMaps, and Kustomize patches.

Primary files:

- `gradle/environments/*.properties`
- `build-logic-settings/src/main/kotlin/com/emme/buildlogic/settings/EnvironmentSettingsPlugin.kt`
- `build-logic/src/main/kotlin/com/emme/buildlogic/deployment/KubernetesDeploymentTarget.kt`
- `build-logic/src/main/kotlin/com/emme/buildlogic/deployment/provider/ComposeProvider.kt`
- `deployment/compose/*`
- `infra/kubernetes/overlays/*`

Evidence:

- Every environment validates successfully.
- No environment is selected implicitly from a tag or namespace.

### Factor 4 — Backing services

Changes:

- Declare PostgreSQL/pgvector, Redis, Keycloak, Kafka, Ollama, and external providers per environment.
- Keep local/regression disposable dependencies separate from staging/prod managed dependencies.
- Add health checks and startup ordering for all disposable services.
- Validate database migration compatibility before rollout.

Primary files:

- `deployment/compose/compose.base.yaml`
- `deployment/compose/compose.regression.yaml`
- `infra/kubernetes/base`
- `infra/kubernetes/overlays/*`
- `infra/kubernetes/jobs/migration-job.yaml`

Evidence:

- Compose waits for healthy dependencies.
- Kubernetes readiness and service discovery work.
- Migration job succeeds before application readiness.

### Factor 5 — Build, release, and run

Changes:

- Enforce Java 25 for all Java compilation and Gradle build logic.
- Add a reliable local Java 25 mise setup instead of relying on an incidental `.gradle/jdks` directory.
- Add a named GraalVM 25 native build profile with explicit `JAVA_HOME`/`GRAALVM_HOME` handling.
- Keep Native Image opt-in and fallback-disabled.
- Build JVM and native backend images plus the frontend image.
- Generate SBOM, vulnerability scan, provenance, and digest outputs.

Primary files:

- `build-logic/src/main/kotlin/com/emme/buildlogic/core/JavaConfiguration.kt`
- `build-logic/src/main/kotlin/emme.java-base.gradle.kts`
- `build-logic/src/main/kotlin/emme.native-image.gradle.kts`
- `scripts/mise/env.d/java.sh`
- `mise.toml`
- `.github/actions/setup-gradle/action.yml`
- `.github/workflows/container-image.yml`
- `deployment/compose/compose.runtime-*.yaml`

Evidence:

- `java -version` reports Java 25 for local/CI Gradle execution.
- `native-image --version` reports GraalVM 25 for the native lane.
- `nativeCompile` succeeds under GraalVM 25.
- Native image has no JVM fallback.
- Backend/frontend image digests are immutable and promotable.

### Factor 6 — Processes

Changes:

- Keep backend and frontend independently deployable.
- Run workloads as non-root with restricted privilege escalation.
- Keep backend processes stateless and database/cache state external.
- Ensure JVM and native images expose the same health and application contract.

Primary files:

- `infra/kubernetes/base/backend-deployment.yaml`
- `infra/kubernetes/base/frontend-deployment.yaml`
- backend/frontend Dockerfiles

Evidence:

- Container security context passes policy checks.
- Restarting a pod does not lose application state.
- Both runtime variants pass the same smoke tests.

### Factor 7 — Port binding and routing

Changes:

- Keep `backend:8081` cluster-internal.
- Keep `frontend:80` as the public application service.
- Add environment-specific Ingress/TLS routing to `frontend:80`.
- Set frontend deployment environment value:

```yaml
EMME_API_UPSTREAM=backend:8081
```

- Verify `emme-web` Vite `API_PROXY_TARGET` and Nginx proxy paths:
  `/api`, `/oauth2`, `/login/oauth2`, and `/q`.
- Disable Nginx buffering and extend timeouts for SSE.

Primary files:

- `infra/kubernetes/base/frontend-deployment.yaml`
- `infra/kubernetes/base/frontend-service.yaml`
- new/updated Ingress resources
- `emme-web/apps/emme-salon-app/vite.config.ts`
- `emme-web/apps/emme-salon-app/nginx.conf.template`

Evidence:

- Browser traffic uses one origin.
- API, OAuth, and SSE work through the frontend host.
- Backend is not directly exposed to browsers.

### Factor 8 — Concurrency

Changes:

- Define per-environment replicas and resource requests/limits.
- Configure HPA for staging/prod.
- Add load-test thresholds for 100 active users and less than 1% errors over 15 minutes.
- Compare JVM/native memory and throughput under equivalent limits.

Primary files:

- `infra/kubernetes/base/backend-hpa.yaml`
- `infra/kubernetes/overlays/*`
- `performance/locust/*`

Evidence:

- HPA responds to load.
- Resource limits prevent uncontrolled consumption.
- Locust results meet the release threshold.

### Factor 9 — Disposability

Changes:

- Add startup, liveness, and readiness probes for backend and frontend.
- Verify graceful termination and rollout windows.
- Make migration ordering explicit.
- Test pod restart, rollout interruption, and recovery.

Primary files:

- `infra/kubernetes/base/backend-deployment.yaml`
- `infra/kubernetes/base/frontend-deployment.yaml`
- `infra/kubernetes/jobs/migration-job.yaml`
- Compose health checks

Evidence:

- Restarted pods recover without manual repair.
- Rollouts stop on failed readiness.
- Startup and shutdown behavior is documented and tested.

### Factor 10 — Development/production parity

Changes:

- Align Compose and Kubernetes service names, API paths, secret keys, and health contracts.
- Run regression browser tests through the frontend origin.
- Keep authentication, OAuth, migrations, and tenant provisioning behavior equivalent.
- Remove environment-specific browser API base URLs for deployed environments.

Primary files:

- `deployment/compose/*`
- `infra/kubernetes/*`
- `emme-web` runtime configuration and E2E providers

Evidence:

- Compose regression and staging smoke tests exercise the same routes.
- Differences are intentional and documented.

### Factor 11 — Logs

Changes:

- Emit backend logs to stdout/stderr with correlation IDs.
- Preserve Nginx access/error logs for operational diagnosis.
- Redact credentials, cookies, authorization headers, and tokens.
- Define environment retention and collection behavior.

Primary files:

- Spring logging configuration
- `emme-web/apps/emme-salon-app/nginx.conf.template`
- Kubernetes logging/observability overlays

Evidence:

- A request can be traced through frontend, Nginx, backend, and dependencies.
- Secret scan finds no credential-bearing logs.

### Factor 12 — Administrative processes

Changes:

- Run Liquibase through explicit migration Jobs.
- Keep tenant provisioning and cleanup as explicit operations.
- Add secret rotation commands for both providers.
- Define backup/restore and rollback procedures.
- Apply least-privilege service accounts and RBAC.

Primary files:

- `infra/kubernetes/jobs/*`
- `tools/e2e-provisioner/*`
- `scripts/*`
- `build-logic/src/main/kotlin/com/emme/buildlogic/secrets/*`

Evidence:

- Migration/provisioning Jobs are auditable and repeatable.
- Secret rotation rolls workloads safely.
- Restore and rollback procedures are rehearsed.

### Factor 13 — API contract

Changes:

- Verify API version headers and OpenAPI output.
- Align frontend API clients and generated/public contracts with backend routes.
- Add contract coverage for authentication, OAuth, CRUD flows, and SSE.
- Ensure proxy paths do not fall through to SPA fallback.

Primary files:

- backend controllers and API contract tests
- `emme-web/packages/api-client/*`
- `emme-web/packages/contracts/*`
- `emme-web/e2e/src/*`

Evidence:

- API docs and contract tests pass.
- Critical browser flows work through the frontend origin.

### Factor 14 — Telemetry

Changes:

- Expose Actuator health and Prometheus metrics.
- Verify Prometheus alerts for availability, error rate, latency, database, Redis, and pod restarts.
- Align Grafana dashboards with canonical environment names.
- Add synthetic frontend-origin health checks.
- Record deployment, rollout, and image digest evidence.

Primary files:

- `infra/kubernetes/base/prometheus-alerts.yaml`
- `infra/kubernetes/base/grafana-dashboard.yaml`
- observability configuration
- CI deployment evidence workflow

Evidence:

- Health and metrics endpoints respond.
- Alerts fire in controlled failure tests.
- Staging and production deployments have telemetry evidence.

### Factor 15 — Automation and security

Changes:

- Update CI for Java 25 Temurin and GraalVM 25.
- Add JVM/native build, scan, SBOM, provenance, and digest gates.
- Support explicit Bitwarden or GitHub Actions secret providers.
- Materialize runtime values only as Kubernetes Secrets.
- Add namespaces, RBAC, NetworkPolicies, TLS, protected production approval, and rollback gates.
- Add cross-repository frontend/backend release coordination.

Primary files:

- `.github/workflows/*`
- `.github/actions/setup-gradle/action.yml`
- `build-logic/src/main/kotlin/com/emme/buildlogic/secrets/*`
- `infra/kubernetes/overlays/*`
- `infra/kubernetes/base/policies/*`
- `emme-web/.github/workflows/*`

Evidence:

- CI can build, scan, deploy, verify, promote, and roll back without undocumented manual steps.
- No plaintext secret reaches Git, an image, a rendered artifact, or logs.
- Production requires explicit authorization.

### Factor 16 — Deployment platform and landing zone

The first production target is self-managed k3s on dedicated Linux VMs. The existing Terraform profile is the starting provider implementation and will be completed for Hetzner Cloud; provider-specific abstractions must not leak into Kubernetes manifests. DigitalOcean remains a future provider profile, not a second release path in this scope.

The approved topology is:

| Environment | Platform | Topology | Primary purpose |
|---|---|---|---|
| `local` | Docker Compose plus optional K3d | Compose is the fast default; K3d is an early Kubernetes-parity lane | Daily development and early deployment-contract detection |
| `dev` | Self-managed k3s | One server node | Fast shared development validation |
| `regression` | Docker Compose | Disposable Compose project | Full REST/UI regression and release-candidate verification |
| `staging` | Self-managed k3s | One server node with production-like manifests | Promotion, telemetry, load, and rollback rehearsal |
| `prod` | Self-managed k3s | Three server nodes with embedded etcd plus worker nodes | High-availability production workload |

The dual local lanes are intentional: Compose gives developers a fast and debuggable default, while K3d detects Kubernetes-specific failures early. Both lanes exercise the same frontend-origin proxy contract. The single-node dev/staging choice reduces cost and operational overhead, while staging must be rebuildable from Terraform and its data must be restorable from tested backups. Production uses an odd number of server nodes because embedded etcd requires quorum. Worker nodes run application workloads; control-plane nodes are tainted unless capacity evidence justifies otherwise.

Infrastructure and workload boundaries are:

- Terraform owns VM instances, private networking, firewalls, floating/public IPs, DNS records, and the S3-compatible backup bucket.
- k3s bootstrap configuration owns the pinned k3s version, server/agent roles, cluster token handling, secrets encryption, cluster CIDR, service CIDR, and registration address.
- Kustomize owns application namespaces, Deployments, Services, Ingress, NetworkPolicies, resource limits, and immutable image digests.
- The bundled k3s Traefik controller owns external HTTP/TLS ingress. It is configured through supported HelmChartConfig resources; generated k3s manifests are never edited in place.
- The frontend Nginx container remains the browser-facing reverse proxy from the public frontend Service to the internal `backend:8081` Service. The backend Service is never exposed as a public LoadBalancer.

Stateful placement is explicit per environment. Dev may use k3s local-path storage for disposable data. Staging must use replicated or dedicated persistent storage and exercise restore. Production PostgreSQL/pgvector, Kafka, and backup/object storage must use dedicated persistent hosts or a tested storage/operator layer; Ollama GPU capacity must use dedicated worker/GPU hosts and must not be scheduled on control-plane nodes. Redis and Keycloak may run in-cluster only when their persistence, replica, and recovery tests pass.

Factor 16 requires these operational controls:

1. Pin the Linux image, k3s version, Terraform providers, VM types, regions, and node labels.
2. Provision each environment through Terraform with no manually created production node.
3. Bootstrap a single server, join staging/dev nodes, and bootstrap production with `cluster-init` plus two additional server nodes.
4. Restrict firewall access: SSH to the operations network, Kubernetes API through a private/fixed registration path, and public traffic only to the ingress ports.
5. Configure DNS, TLS issuance/renewal, ingress health, and certificate expiry alerting.
6. Apply storage classes, quotas, pod disruption budgets, node affinity, taints, and topology spread rules.
7. Enable Kubernetes Secret encryption and materialize runtime secrets through the approved provider contract.
8. Schedule k3s etcd snapshots to encrypted S3-compatible storage and back up the k3s server token with equivalent protection.
9. Back up application data separately from cluster state and execute a documented restore drill.
10. Verify node replacement, server quorum, worker drain, pod rescheduling, and public endpoint recovery.
11. Define a pinned upgrade sequence, pre-upgrade snapshot, canary node, rollback procedure, and maintenance window.
12. Collect cluster, node, ingress, workload, database, and release-bundle evidence for every promotion.
13. Validate capacity, cost, resource requests/limits, and failure behavior before production authorization.
14. Test a rebuild-from-zero in a clean account/project using only Terraform, bootstrap artifacts, backups, and the release bundle.

The deployment target is accepted only when both local lanes pass their contract checks, dev/staging/production can be rendered and reconciled through the same `kubernetes:*` task interface, staging can be restored from backup, production survives one server-node failure without losing etcd quorum, and the release record contains the cluster, node, k3s, overlay, image digest, backup, and rollback evidence.

## 6. Verification Commands

The final release gate will run, with Java 25 selected explicitly:

```bash
mise run toolchain:jvm
for env in local dev regression staging prod; do
  EMME_ENV="$env" mise run env:verify
done
mise run quality:all
EMME_RUNTIME=jvm mise run build:package
EMME_RUNTIME=native mise run build:native
EMME_ENV=regression EMME_RUNTIME=jvm mise run compose:config
EMME_ENV=local EMME_RUNTIME=jvm mise run compose:config
EMME_ENV=local EMME_RUNTIME=jvm mise run kubernetes:render >/dev/null
for env in dev staging prod; do
  for runtime in jvm native; do
    EMME_ENV="$env" EMME_RUNTIME="$runtime" mise run kubernetes:render >/dev/null
  done
done
EMME_ENV=prod EMME_RUNTIME=jvm mise run kubernetes:apply --dry-run
terraform -chdir=infra/terraform validate
EMME_ENV=dev mise run kubernetes:status
EMME_ENV=staging mise run kubernetes:status
EMME_ENV=prod mise run kubernetes:status
```

The web release gate will run:

```bash
bun install --frozen-lockfile
bun run typecheck
bun run lint
bun run test
bun run build
```

Then the real-browser regression suite will exercise the frontend-origin API, OAuth, SSE, and critical user journeys.

## 7. Definition of Done

- All 16 factors have an executable check and stored evidence.
- Java 25 is the only supported service build baseline.
- GraalVM 25 native build succeeds with fallback disabled.
- JVM and native images are scanned and traceable by digest.
- Compose local/regression and Kustomize dev/staging/prod render successfully.
- Kubernetes Secrets are provider-driven and never plaintext.
- Frontend Vite/Nginx proxying works against `backend:8081` without CORS issues.
- Critical browser journeys pass through the frontend origin.
- Staging rollout, telemetry, and rollback are verified.
- Production deployment is protected and reproducible.
- Self-managed k3s dev/staging/prod infrastructure is Terraform-provisioned, backed up, upgradeable, observable, and rebuildable.
- Gradle and mise task names, environment variables, configuration ownership, and migration rules conform to the standardized task contract.
- Existing tests and unrelated user changes remain intact.

## 8. Technical References

- [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Boot Native Image](https://docs.spring.io/spring-boot/reference/packaging/native-image/index.html)
- [GraalVM Native Build Tools Gradle plugin](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html)
- [K3s high-availability embedded etcd](https://docs.k3s.io/datastore/ha-embedded)
- [K3s backup and restore](https://docs.k3s.io/datastore/backup-restore)
- [K3s networking services and Traefik](https://docs.k3s.io/networking/networking-services)
- [K3s rollback procedure](https://docs.k3s.io/upgrades/roll-back)
