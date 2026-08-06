# 15-Factor Release Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement and verify the EMME 15-Factor Release Controls across `emme-service` and `emme-web`, including Java 25, GraalVM 25, canonical environments, Kubernetes Secrets, same-origin proxying, immutable release bundles, and staged promotion.

**Architecture:** `emme-service` owns backend builds, release metadata, Compose, Kustomize, Kubernetes Secrets, ingress, and promotion. `emme-web` owns Vite and the frontend Nginx image source. Compose serves `local` and `regression`; Kustomize serves `dev`, `staging`, and `prod`. A release bundle binds backend/frontend image digests, source SHAs, and API contract version.

**Tech Stack:** Java 25, Gradle 9.4.1, Spring Boot 4.1, GraalVM Community 25, GraalVM Native Build Tools 1.1.5, Kotlin DSL, Docker Compose, Kubernetes, Kustomize, GitHub Actions, Bitwarden/GitHub Actions Secrets, Bun, Vite, Nginx, Vitest, Playwright, JUnit 5, Gradle TestKit, Trivy, Prometheus, Grafana, and Locust.

## Global Constraints

- Canonical environments are exactly `local`, `dev`, `regression`, `staging`, and `prod`.
- Runtime is independently `jvm` or `native`; overlay names follow examples such as `dev-jvm`, `staging-native`, and `prod-jvm`.
- `backend` is the canonical service name in Compose and Kubernetes; the image remains `ghcr.io/migangdelzar/emme-service`.
- Compose is limited to `local` and `regression`; Kubernetes/Kustomize owns `dev`, `staging`, and `prod`.
- Java 25 Temurin runs normal Gradle/JVM builds; explicit GraalVM Community 25 runs Native Image builds.
- Native Image fallback is disabled; the JVM image remains the rollback artifact until native evidence is accepted.
- Kubernetes runtime credentials are namespaced Secret objects; secret values never enter Git, images, rendered source-controlled manifests, or logs.
- Bitwarden and GitHub Actions are explicit secret providers; no silent provider fallback is allowed.
- Staging and production use backend/frontend image digests, never `latest` or mutable release tags.
- Browser API traffic is same-origin through Vite in development and frontend Nginx in deployed environments.
- Every implementation task follows Red → Green → Refactor and ends with a focused commit.
- Existing dirty user changes must remain unstaged and unmodified.
- Use `JAVA_HOME=$(mise exec -- printenv JAVA_HOME)` or the approved Java 25 equivalent for Gradle commands.
- Do not run Gradle test/report writers concurrently in the same checkout.

## File Map

### `emme-service` files to modify or create

- `build-logic/src/main/kotlin/com/emme/buildlogic/environment/EnvironmentName.kt` — canonical environment enum.
- `build-logic/src/main/kotlin/com/emme/buildlogic/environment/task/VerifyEnvironmentTask.kt` — environment contract validation.
- `build-logic/src/main/kotlin/com/emme/buildlogic/deployment/KubernetesDeploymentTarget.kt` — environment/runtime-to-overlay and namespace resolver.
- `build-logic/src/main/kotlin/com/emme/buildlogic/deployment/provider/ComposeProvider.kt` — canonical Compose file selection.
- `build-logic/src/main/kotlin/com/emme/buildlogic/secrets/*` — provider and Kubernetes Secret contract.
- `build-logic/src/main/kotlin/com/emme/buildlogic/core/JavaConfiguration.kt` — Java 25 source/toolchain authority.
- `build-logic/src/main/kotlin/emme.java-base.gradle.kts` — shared Java 25 convention.
- `build-logic/src/main/kotlin/emme.native-image.gradle.kts` — explicit GraalVM native convention.
- `build-logic/src/test/kotlin/com/emme/buildlogic/EnvironmentModelTest.kt` — environment tests.
- `build-logic/src/test/kotlin/com/emme/buildlogic/KubernetesDeploymentTargetTest.kt` — overlay/namespace tests.
- `build-logic/src/test/kotlin/com/emme/buildlogic/SecretsProviderTest.kt` — provider selection and required-key tests.
- `build-logic/src/functionalTest/kotlin/com/emme/buildlogic/JavaBaseConventionFunctionalTest.kt` — Java 25 TestKit checks.
- `build-logic/src/functionalTest/kotlin/com/emme/buildlogic/NativeImageConventionFunctionalTest.kt` — native task/fallback checks.
- `gradle/environments/local.properties` — local contract.
- `gradle/environments/dev.properties` — dev contract.
- `gradle/environments/regression.properties` — regression contract.
- `gradle/environments/staging.properties` — staging contract.
- `gradle/environments/prod.properties` — production contract replacing `production.properties`.
- `gradle/secrets/manifest.json` — canonical secret key declarations.
- `deployment/compose/compose.base.yaml` — shared Compose services.
- `deployment/compose/compose.local.yaml` — local overlay.
- `deployment/compose/compose.regression.yaml` — regression overlay.
- `deployment/compose/compose.runtime-jvm.yaml` — JVM runtime.
- `deployment/compose/compose.runtime-native.yaml` — native runtime.
- `deployment/compose/env/local.env.example` — non-secret local inputs.
- `deployment/compose/env/regression.env.example` — non-secret regression inputs.
- `infra/kubernetes/base/*` — shared service, frontend, Secret references, ingress, probes, policies, and telemetry.
- `infra/kubernetes/overlays/dev-jvm/*` and `dev-native/*` — development overlays.
- `infra/kubernetes/overlays/staging-jvm/*` and `staging-native/*` — staging overlays.
- `infra/kubernetes/overlays/prod-jvm/*` and `prod-native/*` — production overlays.
- `infra/kubernetes/jobs/*` — migrations and administrative jobs.
- `scripts/validate-emme-platform-target.mjs` — canonical deployment validator.
- `scripts/validate-emme-platform-target.test.mjs` — deployment validator tests.
- `scripts/validate-release-bundle.mjs` — release bundle validator.
- `scripts/validate-release-bundle.test.mjs` — release bundle tests.
- `scripts/validate-toolchain.mjs` — Java/GraalVM version validator.
- `scripts/validate-toolchain.test.mjs` — toolchain validator tests.
- `deployment/releases/release.schema.yaml` — release bundle shape.
- `.github/actions/setup-gradle/action.yml` — Java 25 JVM setup.
- `.github/actions/setup-graalvm/action.yml` — reusable GraalVM 25 setup.
- `.github/workflows/ci-backend.yml` — backend checks and render gates.
- `.github/workflows/container-image.yml` — JVM/native image build, SBOM, scan, digest, and provenance.
- `.github/workflows/release.yml` — cross-repository release bundle and promotion.
- `scripts/mise/env.d/java.sh` — local Java 25 selection.
- `mise.toml` — canonical environment/toolchain tasks.
- `docs/architecture/04-delivery/deployment.md` — deployment naming and workflow.
- `docs/architecture/04-delivery/secrets.md` — Secret provider/runtime contract.
- `docs/architecture/04-delivery/native-image.md` — Java 25/GraalVM 25 workflow.

### `emme-web` files to modify or create

- `apps/emme-salon-app/vite.config.ts` — development proxy target and same-origin paths.
- `apps/emme-salon-app/nginx.conf.template` — deployed proxy paths, headers, and SSE behavior.
- `apps/emme-salon-app/Dockerfile` — immutable metadata and safe upstream default.
- `apps/emme-salon-app/docker-compose.yml` — local frontend upstream.
- `apps/emme-salon-app/src/app/config/runtimeConfig.ts` — public same-origin API base contract.
- `apps/emme-salon-app/src/app/config/runtimeConfig.test.ts` — public config tests.
- `packages/api-client/src/client.test.ts` — same-origin API URL tests.
- `e2e/src/playwright.config.ts` — frontend-origin API and proxy environment.
- `e2e/src/specs/cross-cutting/proxy-contract.spec.ts` — browser-origin proxy contract.
- `.github/workflows/real-e2e-recordings.yml` — cross-repository release inputs.
- `scripts/validate-proxy-config.mjs` — Nginx/Vite static contract validator.
- `scripts/validate-proxy-config.test.mjs` — proxy validator tests.

## Dependency Graph

```text
canonical environment model
  ├── Gradle/mise selection
  ├── Compose file selection
  ├── Kustomize overlay selection
  ├── namespace/hostname selection
  └── Secret project selection

Java/GraalVM toolchain
  ├── JVM build
  ├── native build
  └── container image build

release bundle
  ├── regression verification
  ├── staging promotion
  ├── production promotion
  └── rollback

frontend proxy contract
  ├── Vite local development
  ├── Nginx container
  ├── Kubernetes frontend Deployment
  └── browser E2E
```

## Phase 0: Release Preconditions

### Task 0: Establish the Java 25/GraalVM 25 execution baseline

**Files:**

- Modify: `scripts/mise/env.d/java.sh`
- Modify: `mise.toml`
- Create: `.github/actions/setup-graalvm/action.yml`
- Create: `scripts/validate-toolchain.mjs`
- Create: `scripts/validate-toolchain.test.mjs`

**Interfaces:**

- Produces `JAVA_HOME`, `GRAALVM_HOME`, `java -version`, and `native-image --version` validation outputs for later tasks.

- [ ] **Step 1: Write failing tests**

Add tests that reject Java versions other than 25, reject a missing `native-image` command for the native lane, and accept a JVM-only validation mode without GraalVM.

- [ ] **Step 2: Run the tests and confirm failure**

Run:

```bash
node --test scripts/validate-toolchain.test.mjs
```

Expected: FAIL because the validator does not exist.

- [ ] **Step 3: Implement the validator and local task**

Implement a shell-safe Node validator with two modes:

```text
node scripts/validate-toolchain.mjs --jvm
node scripts/validate-toolchain.mjs --native
```

Update mise so Java 25 is selected deterministically and add `toolchain:jvm` and `toolchain:native` tasks that call the validator.

- [ ] **Step 4: Run focused verification**

```bash
node --test scripts/validate-toolchain.test.mjs
mise run toolchain:jvm
mise run toolchain:native
```

Expected: Java 25 passes; native mode passes only when GraalVM 25 is installed and fails with an actionable message otherwise.

- [ ] **Step 5: Commit**

```bash
git add scripts/mise/env.d/java.sh mise.toml scripts/validate-toolchain.* .github/actions/setup-graalvm/action.yml
git commit -m "build(toolchain): standardize Java 25 and GraalVM 25"
```

## Factor Tasks

### Task 1: Codebase provenance and release metadata

**Files:**

- Create: `deployment/releases/release.schema.yaml`
- Create: `scripts/validate-release-bundle.mjs`
- Create: `scripts/validate-release-bundle.test.mjs`
- Modify: `build-logic/src/main/kotlin/com/emme/buildlogic/container/ContainerPlugin.kt`
- Modify: `build-logic/src/main/kotlin/com/emme/buildlogic/container/task/BuildContainerImageTask.kt`
- Modify: `build-logic/src/main/kotlin/com/emme/buildlogic/container/task/VerifyContainerImageTask.kt`
- Modify: `build-logic/src/main/kotlin/com/emme/buildlogic/publishing/task/GenerateReleaseManifestTask.kt`
- Modify: `build-logic/src/main/kotlin/com/emme/buildlogic/publishing/task/GenerateSbomTask.kt`
- Modify: `emme-web/apps/emme-salon-app/Dockerfile`

**Interfaces:**

- Release bundle accepts backend/frontend image digests, source SHAs, API contract version, and release identifier.
- Validator returns non-zero for missing fields, mutable tags, invalid digests, or mismatched API contract.

- [ ] **Step 1: Write failing release bundle tests**

Test valid and invalid YAML fixtures, including missing frontend digest, `latest` references, malformed SHA-256 digests, and missing source SHAs.

- [ ] **Step 2: Run tests and confirm failure**

```bash
node --test scripts/validate-release-bundle.test.mjs
```

Expected: FAIL because the schema/validator is absent.

- [ ] **Step 3: Implement schema, validator, and image labels**

Use the release shape defined in the specification. Add OCI labels for source repository, source SHA, release identifier, and build timestamp to both images.

- [ ] **Step 4: Verify**

```bash
node --test scripts/validate-release-bundle.test.mjs
BACKEND_IMAGE_REF="ghcr.io/migangdelzar/emme-service:sha-${GITHUB_SHA}"
FRONTEND_IMAGE_REF="ghcr.io/migangdelzar/emme-web:sha-${GITHUB_SHA}"
docker inspect "$BACKEND_IMAGE_REF" --format '{{json .Config.Labels}}'
docker inspect "$FRONTEND_IMAGE_REF" --format '{{json .Config.Labels}}'
```

- [ ] **Step 5: Commit**

```bash
git add deployment/releases scripts/validate-release-bundle*
git commit -m "release(provenance): add backend and frontend release bundle"
git -C ../emme-web add apps/emme-salon-app/Dockerfile
git -C ../emme-web commit -m "release(provenance): add frontend image metadata"
```

### Task 2: Dependency reproducibility and security checks

**Files:**

- Modify: `.github/workflows/ci-backend.yml`
- Modify: `.github/workflows/container-image.yml`
- Modify: `../emme-web/.github/workflows/ci-frontend.yml`
- Modify: `gradle/verification-metadata.xml` only when generated by a verified dependency update
- Modify: `gradle/libs.versions.toml` only for required compatible versions
- Modify: frontend lockfile only through `bun install --frozen-lockfile`/approved update
- Modify: `scripts/validate-container-workflow.mjs`
- Create: `scripts/validate-dependency-workflow.mjs`
- Create: `scripts/validate-dependency-workflow.test.mjs`

- [ ] **Step 1: Write failing contract checks**

Assert that CI invokes locked Gradle and Bun installs, dependency scanning, image scanning, and SBOM generation.

- [ ] **Step 2: Run the contract checks**

```bash
node --test scripts/validate-dependency-workflow.test.mjs
```

Expected: FAIL for missing lockfile/SBOM/security steps.

- [ ] **Step 3: Implement the CI checks**

Use existing lockfiles and verification metadata. Do not add a dependency when the current stack already provides the capability.

- [ ] **Step 4: Verify**

```bash
JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew dependencies --no-daemon --no-configuration-cache
cd ../emme-web && bun install --frozen-lockfile
```

- [ ] **Step 5: Commit**

```bash
git add .github gradle scripts
git commit -m "ci(dependencies): enforce reproducible backend and frontend inputs"
git -C ../emme-web add package.json bun.lock .github
git -C ../emme-web commit -m "ci(dependencies): enforce reproducible frontend inputs"
```

### Task 3: Canonical environment model

**Files:**

- Modify: `build-logic/src/main/kotlin/com/emme/buildlogic/environment/EnvironmentName.kt`
- Modify: `build-logic/src/main/kotlin/com/emme/buildlogic/environment/task/VerifyEnvironmentTask.kt`
- Modify: `build-logic/src/main/kotlin/com/emme/buildlogic/deployment/KubernetesDeploymentTarget.kt`
- Modify: `build-logic/src/main/kotlin/com/emme/buildlogic/deployment/provider/ComposeProvider.kt`
- Rename: `gradle/environments/production.properties` → `gradle/environments/prod.properties`
- Modify: `gradle/environments/*.properties`
- Rename: `applications/emme-platform/src/main/resources/application-production.yml` → `application-prod.yml`
- Rename or consolidate: `applications/emme-platform/src/main/resources/application-e2e.yml` into the canonical regression profile contract
- Modify: `applications/emme-platform/src/main/resources/application.yml`
- Modify: `build-logic/src/test/kotlin/com/emme/buildlogic/EnvironmentModelTest.kt`
- Modify: `build-logic/src/test/kotlin/com/emme/buildlogic/KubernetesDeploymentTargetTest.kt`
- Modify: `scripts/validate-emme-platform-target.*`

**Interfaces:**

```kotlin
enum class EnvironmentName(val id: String) {
  LOCAL("local"), DEV("dev"), REGRESSION("regression"), STAGING("staging"), PROD("prod")
}
```

`KubernetesDeploymentTarget.overlayName(environment, runtime)` returns values such as `dev-jvm` and `prod-native`; `namespace(environment)` returns values such as `emme-dev` and `emme-prod`.

- [ ] **Step 1: Add red tests**

Test all five canonical names, reject `production`/`test`/`ci`, verify `dev-jvm`, `staging-native`, and `prod-jvm`, and verify Compose selects only `local` or `regression` overlays.

- [ ] **Step 2: Run tests and confirm failure**

```bash
JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew :build-logic:test --tests '*EnvironmentModelTest' --tests '*KubernetesDeploymentTargetTest' --no-daemon --no-configuration-cache
```

- [ ] **Step 3: Implement canonical model**

Remove obsolete environment enum values and derive overlay/namespace names from environment plus runtime. Remove the duplicated `kustomize.overlay` input or validate it against the derived value.

- [ ] **Step 4: Verify all environment reports**

```bash
for env in local dev regression staging prod; do
  JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew verifyEnvironment -Penvironment="$env" --no-daemon --no-configuration-cache
done
```

- [ ] **Step 5: Commit**

```bash
git add build-logic gradle/environments scripts/validate-emme-platform-target.*
git commit -m "refactor(environment): standardize canonical release environments"
```

### Task 4: Backing service and Compose standardization

**Files:**

- Rename: `deployment/compose/compose.yaml` → `deployment/compose/compose.base.yaml`
- Rename: `compose.environment-local.yaml` → `compose.local.yaml`
- Rename: `compose.environment-regression.yaml` → `compose.regression.yaml`
- Remove or migrate: `compose.environment-ci.yaml`, `compose.environment-e2e.yaml`, and `.bak` compatibility files
- Modify: `compose.runtime-jvm.yaml`, `compose.runtime-native.yaml`, `compose.observability.yaml`, `compose.kafka.contract.test.mjs`, `compose.e2e.contract.test.mjs`
- Modify: `deployment/compose/env/*.env.example`
- Modify: `build-logic` Compose provider and `mise.toml`

- [ ] **Step 1: Update contract tests to require new paths and service name**

Assert that the base service is `backend`, required PostgreSQL/Redis health checks exist, and regression has no insecure password fallback.

- [ ] **Step 2: Run tests and confirm failure**

```bash
node --test deployment/compose/compose.e2e.contract.test.mjs deployment/compose/compose.kafka.contract.test.mjs
```

- [ ] **Step 3: Rename and update Compose layers**

Use `${DB_PASSWORD:?DB_PASSWORD is required}` and equivalent required-variable syntax for credentials. Set the Compose project name from the canonical environment.

- [ ] **Step 4: Verify configuration**

```bash
docker compose -f deployment/compose/compose.base.yaml -f deployment/compose/compose.runtime-jvm.yaml -f deployment/compose/compose.local.yaml config
docker compose -f deployment/compose/compose.base.yaml -f deployment/compose/compose.runtime-jvm.yaml -f deployment/compose/compose.regression.yaml config
```

- [ ] **Step 5: Commit**

```bash
git add deployment/compose build-logic mise.toml
git commit -m "refactor(compose): standardize local and regression environments"
```

### Task 5: Kubernetes overlay and namespace standardization

**Files:**

- Rename: `infra/kubernetes/overlays/k3d-jvm` → `dev-jvm`
- Rename: `infra/kubernetes/overlays/k3d-native` → `dev-native`
- Rename: `infra/kubernetes/overlays/k3s-production-jvm` → `prod-jvm`
- Rename: `infra/kubernetes/overlays/k3s-production-native` → `prod-native`
- Create: `infra/kubernetes/overlays/staging-jvm/kustomization.yaml`
- Create: `infra/kubernetes/overlays/staging-native/kustomization.yaml`
- Modify: `infra/kubernetes/base/namespace.yaml`
- Modify: `KubernetesDeploymentTargetTest.kt`, mise tasks, CI validators, and all workflow references

- [ ] **Step 1: Add red overlay matrix tests**

Require all six overlay directories and reject legacy overlay paths in active workflows.

- [ ] **Step 2: Run tests and confirm failure**

```bash
node --test scripts/validate-emme-platform-target.test.mjs
```

- [ ] **Step 3: Rename/create overlays**

Each overlay must set environment profile, namespace, backend/frontend image inputs, resources, replicas, hostname/TLS, and runtime-specific settings without duplicating the base.

- [ ] **Step 4: Render all overlays**

```bash
for overlay in dev-jvm dev-native staging-jvm staging-native prod-jvm prod-native; do
  kubectl kustomize "infra/kubernetes/overlays/$overlay" >/dev/null
done
```

- [ ] **Step 5: Commit**

```bash
git add infra/kubernetes mise.toml .github scripts
git commit -m "refactor(kubernetes): standardize environment runtime overlays"
```

### Task 6: Kubernetes Secret contract and providers

**Files:**

- Modify: `gradle/secrets/manifest.json`
- Modify: `build-logic/src/main/kotlin/com/emme/buildlogic/secrets/*`
- Modify: `build-logic/src/test/kotlin/com/emme/buildlogic/SecretsProviderTest.kt`
- Create: `scripts/materialize-kubernetes-secrets.mjs`
- Create: `scripts/materialize-kubernetes-secrets.test.mjs`
- Modify: `infra/kubernetes/base/backend-deployment.yaml`
- Create/modify: Secret reference overlays and deployment documentation

**Interfaces:**

The existing provider port remains the validation boundary and is extended with
an explicit in-memory resolution operation; provider selection is explicit for
staging and production:

```kotlin
interface SecretProvider {
  val kind: SecretProviderKind
  fun validate(requiredNames: Set<String>): Set<String>
  fun resolve(requiredNames: Set<String>): Map<String, String>
}

interface KubernetesSecretMaterializer {
  fun apply(namespace: String, secretName: String, values: Map<String, String>): SecretMaterializationReport
}
```

- [ ] **Step 1: Add red provider/materializer tests**

Cover explicit Bitwarden selection, explicit GitHub Actions selection, missing provider, missing required key, namespace mismatch, and output redaction.

- [ ] **Step 2: Run tests and confirm failure**

```bash
JAVA_HOME=$(mise exec -- printenv JAVA_HOME) ./gradlew :build-logic:test --tests '*SecretsProviderTest' --no-daemon --no-configuration-cache
node --test scripts/materialize-kubernetes-secrets.test.mjs
```

- [ ] **Step 3: Implement the contract**

Use the four Secret objects defined in the specification. Resolve values from the selected provider and apply them with `kubectl create secret generic ... --dry-run=client -o yaml | kubectl apply -f -`. Never print values.

- [ ] **Step 4: Replace plaintext deployment values**

Use stable `secretKeyRef` entries instead of positional environment removal patches.

- [ ] **Step 5: Verify failure-safe behavior**

```bash
node scripts/materialize-kubernetes-secrets.mjs --environment=staging --provider=github-actions --dry-run
```

Expected: missing required credentials causes a non-zero exit without applying workload resources.

- [ ] **Step 6: Commit**

```bash
git add gradle/secrets build-logic/src/main/kotlin/com/emme/buildlogic/secrets build-logic/src/test/kotlin/com/emme/buildlogic/SecretsProviderTest.kt scripts/materialize-kubernetes-secrets.* infra/kubernetes docs/architecture/04-delivery/secrets.md
git commit -m "feat(secrets): materialize canonical Kubernetes Secret contracts"
```

### Task 7: Frontend proxy and Kubernetes ingress

**Files:**

- Modify: `infra/kubernetes/base/frontend-deployment.yaml`
- Modify: `infra/kubernetes/base/frontend-service.yaml`
- Create: `infra/kubernetes/base/ingress.yaml`
- Modify: all environment overlays for `EMME_API_UPSTREAM=backend:8081`
- Modify in `emme-web`: `vite.config.ts`, `nginx.conf.template`, `Dockerfile`, proxy tests, and runtime config tests
- Create in `emme-web`: `scripts/validate-proxy-config.mjs` and test

- [ ] **Step 1: Add red proxy contract tests**

Assert Vite proxies `/api`, OAuth paths, and `/q`; assert Nginx contains same locations, forwarded headers, `proxy_buffering off`, long SSE timeout, and no SPA fallback for API paths. Assert every Kubernetes frontend overlay sets `backend:8081`.

- [ ] **Step 2: Run tests and confirm failure**

```bash
cd ../emme-web
node --test scripts/validate-proxy-config.test.mjs
```

- [ ] **Step 3: Implement frontend and Kubernetes routing**

Use `frontend:80` as the only public service. Add `networking.k8s.io/v1` Ingress with explicit class, host, and TLS behavior per environment. Keep `backend:8081` ClusterIP-only.

- [ ] **Step 4: Verify routing**

```bash
cd ../emme-web
bun run test
bun run build
node --test scripts/validate-proxy-config.test.mjs
```

Then run frontend-origin browser tests against `/api`, OAuth, and SSE.

- [ ] **Step 5: Commit web and service changes separately**

```bash
git add apps/emme-salon-app packages e2e scripts/validate-proxy-config*
git commit -m "feat(proxy): enforce same-origin frontend API routing"
```

In `emme-service`:

```bash
git add infra/kubernetes
git commit -m "feat(ingress): route public traffic through frontend"
```

### Task 8: Process security, resources, and concurrency

**Files:**

- Modify: `infra/kubernetes/base/backend-deployment.yaml`
- Modify: `infra/kubernetes/base/frontend-deployment.yaml`
- Modify: `infra/kubernetes/base/backend-hpa.yaml`
- Modify: `infra/kubernetes/overlays/*`
- Modify: `performance/locust/*`
- Create/modify: manifest security/resource validator and tests

- [ ] **Step 1: Add manifest tests**

Require non-root containers, no privilege escalation, resource requests/limits, environment-specific replicas, and HPA only where supported.

- [ ] **Step 2: Run tests and confirm failure**

```bash
node --test scripts/validate-emme-platform-target.test.mjs
```

- [ ] **Step 3: Implement resources and HPA**

Set explicit JVM/native resource budgets and staging/prod replica policy. Keep local/dev disposable resources bounded.

- [ ] **Step 4: Run load verification**

```bash
locust -f performance/locust --headless --users 100 --spawn-rate 10 --run-time 15m
```

Expected: fewer than 1% server errors and captured latency/resource evidence.

- [ ] **Step 5: Commit**

```bash
git add infra/kubernetes performance/locust scripts
git commit -m "perf(runtime): define release concurrency and resource gates"
```

### Task 9: Probes, migrations, rollout, and disposability

**Files:**

- Modify: `infra/kubernetes/base/backend-deployment.yaml`
- Modify: `infra/kubernetes/base/frontend-deployment.yaml`
- Modify: `infra/kubernetes/jobs/migration-job.yaml`
- Modify: `infra/kubernetes/jobs/kustomization.yaml`
- Modify: Compose health checks and deployment provider status handling

- [ ] **Step 1: Add failing rollout contract tests**

Require startup/readiness/liveness probes, termination grace period, migration Job dependency, and frontend `/health` probe.

- [ ] **Step 2: Run tests and confirm failure**

```bash
node --test scripts/validate-emme-platform-target.test.mjs
```

- [ ] **Step 3: Implement probe and rollout contracts**

Use stable named paths and avoid environment-variable array-index patches. The migration Job must finish before backend readiness is accepted.

- [ ] **Step 4: Verify live behavior**

```bash
kubectl rollout status deployment/backend -n emme-staging --timeout=5m
kubectl rollout status deployment/frontend -n emme-staging --timeout=5m
kubectl delete pod -l app=emme-backend -n emme-staging
kubectl rollout status deployment/backend -n emme-staging --timeout=5m
```

- [ ] **Step 5: Commit**

```bash
git add infra/kubernetes deployment/compose build-logic/src/main/kotlin/com/emme/buildlogic/deployment
git commit -m "fix(runtime): make rollout and recovery checks explicit"
```

### Task 10: Compose/Kubernetes parity

**Files:**

- Modify: `deployment/compose/*`
- Modify: `infra/kubernetes/*`
- Modify: `deployment/compose/*.contract.test.mjs`
- Modify in `emme-web`: `e2e/src/playwright.config.ts`, real provider, and cross-cutting proxy spec

- [ ] **Step 1: Add parity assertions**

Assert common service name `backend`, port 8081, API paths, Secret key names, health paths, and frontend-origin behavior in both deployment targets.

- [ ] **Step 2: Run tests and confirm failure**

```bash
node --test deployment/compose/*.contract.test.mjs
cd ../emme-web/e2e/src && bun run test -- --grep @proxy-contract
```

- [ ] **Step 3: Normalize intentional differences**

Document only differences that are inherent to Compose versus Kubernetes, such as host upstream versus `backend:8081`, and keep the browser contract identical.

- [ ] **Step 4: Verify regression**

```bash
docker compose -f deployment/compose/compose.base.yaml -f deployment/compose/compose.runtime-jvm.yaml -f deployment/compose/compose.regression.yaml up -d --wait
cd ../emme-web/e2e/src && bun run test:real -- --grep @smoke
```

- [ ] **Step 5: Commit**

```bash
git add deployment/compose infra/kubernetes
git commit -m "test(parity): verify Compose and Kubernetes runtime contracts"
git -C ../emme-web add e2e
git -C ../emme-web commit -m "test(parity): verify browser runtime contract"
```

### Task 11: Logging and redaction

**Files:**

- Modify: `applications/emme-platform/src/main/resources/application.yml`
- Modify: `applications/emme-platform/src/main/resources/application-local.yml`
- Modify: `applications/emme-platform/src/main/resources/application-test.yml`
- Modify: `applications/emme-platform/src/main/resources/application-production.yml` before renaming it to `application-prod.yml`
- Modify: `emme-web/apps/emme-salon-app/nginx.conf.template`
- Create: `scripts/validate-log-redaction.mjs`
- Create: `scripts/validate-log-redaction.test.mjs`
- Modify observability documentation.

- [ ] **Step 1: Add redaction tests**

Reject log configuration that emits authorization headers, cookies, passwords, tokens, or encryption keys. Require correlation/request ID propagation.

- [ ] **Step 2: Run tests and confirm failure**

```bash
node --test scripts/validate-log-redaction.test.mjs
```

- [ ] **Step 3: Implement logging contract**

Keep application and Nginx logs on stdout/stderr, configure correlation IDs, and preserve useful access/error diagnostics without credential values.

- [ ] **Step 4: Verify**

```bash
kubectl logs deployment/backend -n emme-staging
kubectl logs deployment/frontend -n emme-staging
```

- [ ] **Step 5: Commit**

```bash
git add scripts/validate-log-redaction* docs/architecture infra/kubernetes
git commit -m "security(logging): enforce correlation and secret redaction"
git -C ../emme-web add apps/emme-salon-app
git -C ../emme-web commit -m "security(logging): enforce frontend log redaction"
```

### Task 12: Administrative jobs and recovery operations

**Files:**

- Modify: `infra/kubernetes/jobs/*`
- Modify: `tools/e2e-provisioner/*`
- Modify: `scripts/e2e-clean.sh`
- Modify: `build-logic/src/main/kotlin/com/emme/buildlogic/secrets/*`
- Create: administrative RBAC manifests and contract tests.

- [ ] **Step 1: Add red job/RBAC tests**

Require separate service accounts, least-privilege verbs, explicit migration/provisioning commands, and no administrative work in the HTTP process.

- [ ] **Step 2: Run tests and confirm failure**

```bash
node --test scripts/validate-admin-manifests.test.mjs
```

- [ ] **Step 3: Implement jobs, rotation, and rollback helpers**

Keep migration, tenant provisioning, secret rotation, backup/restore, and cleanup as explicit operations with auditable output.

- [ ] **Step 4: Verify**

```bash
kubectl apply --dry-run=server -k infra/kubernetes/jobs
kubectl get role,rolebinding,serviceaccount -n emme-staging
```

- [ ] **Step 5: Commit**

```bash
git add infra/kubernetes/jobs scripts tools/e2e-provisioner build-logic/src/main/kotlin/com/emme/buildlogic/secrets
git commit -m "ops(admin): isolate migrations provisioning and recovery jobs"
```

### Task 13: API and frontend contract

**Files:**

- Modify backend API contract tests and OpenAPI configuration.
- Modify: `emme-web/packages/api-client/*`
- Modify: `emme-web/packages/contracts/*`
- Modify: `emme-web/e2e/src/*`
- Create/update cross-repository API contract validator.

- [ ] **Step 1: Add failing contract tests**

Cover API version `1.0`, authentication, OAuth callback paths, tenant isolation, critical CRUD operations, and SSE through the frontend origin.

- [ ] **Step 2: Run tests and confirm failure**

```bash
cd ../emme-web
bun run test -- packages/api-client/src/client.test.ts
```

- [ ] **Step 3: Implement contract alignment**

Keep `VITE_API_BASE_URL` public and same-origin in deployed builds. Keep upstream addresses server-side only. Update clients only through public backend contracts.

- [ ] **Step 4: Verify**

```bash
curl -f https://staging.emme.app/api-docs
cd ../emme-web/e2e/src && bun run test:real -- --grep '@api-contract|@proxy-contract'
```

- [ ] **Step 5: Commit**

```bash
git add modules applications docs
git commit -m "test(api): enforce frontend and backend release contracts"
git -C ../emme-web add packages e2e
git -C ../emme-web commit -m "test(api): enforce frontend and backend release contracts"
```

### Task 14: Telemetry and release evidence

**Files:**

- Modify: `infra/kubernetes/base/prometheus-alerts.yaml`
- Modify: `infra/kubernetes/base/grafana-dashboard.yaml`
- Modify: health/metrics configuration.
- Create: deployment evidence collector and test.
- Modify: `.github/workflows/ci-backend.yml` and release workflow.

- [ ] **Step 1: Add telemetry contract tests**

Require Actuator health, Prometheus metrics, alert groups, dashboard environment labels, rollout status, image digests, and release bundle references.

- [ ] **Step 2: Run tests and confirm failure**

```bash
node --test scripts/validate-telemetry-contract.test.mjs
```

- [ ] **Step 3: Implement telemetry and evidence collection**

Collect only non-secret deployment metadata: environment, source SHAs, image digests, overlay, cluster context, rollout status, health status, and test report links.

- [ ] **Step 4: Verify**

```bash
curl -f https://staging.emme.app/actuator/health
curl -f https://staging.emme.app/actuator/prometheus
kubectl get prometheusrules -n emme-staging
```

- [ ] **Step 5: Commit**

```bash
git add infra/kubernetes .github scripts docs/architecture
git commit -m "ops(telemetry): collect release health and deployment evidence"
```

### Task 15: CI/CD security, promotion, and rollback

**Files:**

- Modify: `.github/actions/setup-gradle/action.yml`
- Create/modify: `.github/actions/setup-graalvm/action.yml`
- Modify: `.github/workflows/ci-backend.yml`
- Modify: `.github/workflows/container-image.yml`
- Create: `.github/workflows/release.yml`
- Modify: `emme-web/.github/workflows/real-e2e-recordings.yml`
- Modify: `infra/kubernetes/overlays/*`
- Create: network policies, TLS, and production approval checks.

- [ ] **Step 1: Add failing workflow contract tests**

Require Java 25, GraalVM 25 native lane, frontend build, locked installs, SBOM, Trivy, digest output, release bundle validation, secret provider selection, staging approval, production approval, and rollback command.

- [ ] **Step 2: Run tests and confirm failure**

```bash
node --test scripts/validate-backend-workflow.mjs scripts/validate-container-workflow.mjs scripts/validate-release-bundle.test.mjs
```

- [ ] **Step 3: Implement workflow and security gates**

Use GitHub environment protection for staging/prod. Require the release bundle to pass validation before any apply. Apply Secrets before workloads, apply migrations before readiness, and stop promotion on any failed gate.

- [ ] **Step 4: Verify render and dry-run gates**

```bash
for overlay in dev-jvm dev-native staging-jvm staging-native prod-jvm prod-native; do
  kubectl kustomize "infra/kubernetes/overlays/$overlay" >/dev/null
done
kubectl apply --dry-run=server -k infra/kubernetes/overlays/prod-jvm
```

- [ ] **Step 5: Rehearse rollback**

Deploy a known-good staging bundle, promote a deliberately failing candidate to staging, verify promotion stops, restore the previous backend/frontend digest pair, and capture rollout/health evidence.

- [ ] **Step 6: Commit**

```bash
git add .github infra/kubernetes scripts deployment/releases docs/architecture
git commit -m "ci(release): enforce secure staged promotion and rollback"
```

## Final Release Gate

- [ ] Run all service unit, integration, architecture, formatting, dependency, and security checks with Java 25.
- [ ] Run GraalVM 25 `nativeCompile`, native startup, health, and smoke checks.
- [ ] Run frontend typecheck, lint, unit tests, build, and proxy validator.
- [ ] Validate local and regression Compose configuration.
- [ ] Render all six Kustomize runtime overlays.
- [ ] Validate Secret contracts without printing values.
- [ ] Validate both backend/frontend digests and release bundle.
- [ ] Run regression browser tests through the frontend origin.
- [ ] Run staging rollout, telemetry, load, and rollback evidence.
- [ ] Verify production approval and immutable promotion controls.
- [ ] Update the 15-factor evidence matrix with command, artifact, threshold, owner, and result.
- [ ] Update deployment, secrets, native-image, release, and web proxy documentation.
- [ ] Commit each logical task and push both repository branches.

## Definition of Done

- Every Factor 1–15 has a passing executable check and stored evidence artifact.
- No canonical environment or deployment path depends on an obsolete name.
- Java 25 is the only JVM build baseline; GraalVM 25 native builds are explicit and fallback-disabled.
- Backend/frontend releases are paired by immutable digest and source provenance.
- Kubernetes Secrets are provider-driven, namespaced, validated, and never plaintext.
- Frontend browser traffic is same-origin in local development, regression, staging, and production.
- Backend is cluster-internal and frontend is the public application boundary.
- Staging deployment, telemetry, load, and rollback are verified.
- Production deployment requires protected authorization and immutable release evidence.
- Existing user worktree changes remain untouched.
