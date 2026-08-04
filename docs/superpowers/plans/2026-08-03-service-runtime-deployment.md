# Service Runtime Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `emme-service` the single source of truth for service images, Compose runtime overlays, K3d/K3s manifests, and disposable E2E infrastructure.

**Architecture:** Gradle/build-logic owns image build and verification tasks. Docker Compose owns disposable local/CI runtime composition. Kustomize owns Kubernetes manifests and overlays. GitHub Actions publishes immutable images and deploys through protected environments or GitOps; production is never built inside K3s.

**Tech Stack:** Gradle 9, Kotlin build logic, Spring Boot 4, Docker, Docker Compose, Kustomize, K3d, K3s, Keycloak, PostgreSQL, Liquibase, GitHub Actions.

## Global Constraints

- Service runtime configuration is owned by `emme-service`; `emme-web` must not duplicate backend infrastructure.
- Image tags used by CI and production are immutable `sha-<git-sha>` tags or resolved digests.
- `latest` is permitted only for local convenience.
- JVM is the default runtime; Native is an explicit separately verified variant.
- Typed Java tools own dynamic E2E provisioning; shell is limited to migration wrappers and thin local bootstrap adapters.
- Kubernetes manifests are declarative and are applied with Kustomize.
- Credentials never appear in committed Compose, Kubernetes, or realm files.
- Every changed file is formatted, validated, committed, and pushed.

---

### Task 1: Normalize the service deployment vocabulary

**Files:**
- Modify: `deployment/compose/compose.yml`
- Modify: `deployment/compose/compose.jvm.yml`
- Modify: `deployment/compose/compose.native.yml`
- Modify: `deployment/compose/compose.local.yml`
- Modify: `deployment/compose/compose.test.yml`
- Modify: `deployment/compose/compose.observability.yml`
- Modify: `deployment/compose/compose.e2e.yml`
- Modify: `deployment/kubernetes/overlays/local/kustomization.yml`
- Modify: `deployment/kubernetes/overlays/production/kustomization.yml`
- Modify: `infra/kubernetes/overlays/dev/kustomization.yaml`
- Modify: `infra/kubernetes/overlays/dev-native/kustomization.yaml`
- Modify: `infra/kubernetes/overlays/prod/kustomization.yaml`
- Modify: `infra/kubernetes/overlays/prod-native/kustomization.yaml`
- Modify: `applications/emme-platform/build.gradle.kts`
- Modify: `.github/workflows/ci-backend.yml`
- Test: `scripts/validate-deployment-layout.mjs`

**Interfaces:**
- Produces one canonical Compose base, explicit runtime overlays, and explicit environment overlays.
- Produces one canonical image environment variable: `EMME_SERVICE_IMAGE`.
- Produces normalized Kubernetes target names: `k3d-jvm`, `k3d-native`, `k3s-staging-jvm`, `k3s-staging-native`, `k3s-production-jvm`, and `k3s-production-native`.

- [ ] **Step 1: Write the failing layout test.**

Create a Node script that asserts the canonical Compose and Kustomize paths exist, old ambiguous names do not remain referenced, and every overlay contains the normalized service image name.

- [ ] **Step 2: Run the layout test.**

Run:

```bash
node scripts/validate-deployment-layout.mjs
```

Expected: FAIL because the repository still uses `compose.yml`, runtime-only names, and legacy environment names.

- [ ] **Step 3: Rename files and update references.**

Use these names:

```text
compose.yaml
compose.runtime-jvm.yaml
compose.runtime-native.yaml
compose.environment-local.yaml
compose.environment-ci.yaml
compose.environment-e2e.yaml
compose.observability.yaml
```

Use `git mv` for tracked renames, then update every workflow, README, Gradle provider, and comment reference. Rename Kubernetes overlays to include target and runtime explicitly.

- [ ] **Step 4: Run the layout and rendering checks.**

Run:

```bash
node scripts/validate-deployment-layout.mjs
docker compose -f deployment/compose/compose.yaml -f deployment/compose/compose.runtime-jvm.yaml config --quiet
docker compose -f deployment/compose/compose.yaml -f deployment/compose/compose.runtime-native.yaml config --quiet
kubectl kustomize infra/kubernetes/overlays/k3d-jvm >/dev/null
kubectl kustomize infra/kubernetes/overlays/k3s-production-jvm >/dev/null
```

Expected: PASS with no legacy deployment file references.

- [ ] **Step 5: Commit.**

```bash
git add deployment infra applications/emme-platform/build.gradle.kts .github/workflows/ci-backend.yml scripts/validate-deployment-layout.mjs
git commit -m "refactor(deployment): normalize runtime and cluster naming"
```

### Task 2: Make the E2E Compose runtime execute the service image

**Files:**
- Modify: `deployment/compose/compose.e2e.yml`
- Modify: `deployment/compose/compose.runtime-jvm.yaml`
- Create: `tools/e2e-provisioner/`
- Test: `deployment/compose/compose.e2e.contract.test.mjs`

**Interfaces:**
- Consumes `EMME_SERVICE_IMAGE`, `E2E_TENANT_SLUG`, and Keycloak E2E secrets. The typed provisioning tool consumes the database and Keycloak endpoints through environment-backed configuration.
- Produces a healthy `emme-platform` container reachable on host port `8081`.
- Produces the exact Keycloak user UUID in memory for membership seeding.

- [x] **Step 1: Add Compose contract assertions.**

The test must parse `docker compose config --format json` and assert that the E2E overlay contains `keycloak`, `database-migrations`, and `emme-platform`; the platform service uses `EMME_SERVICE_IMAGE`; and its container environment points to `postgres`, `redis`, and `keycloak`, not `localhost`.

- [x] **Step 2: Run the contract test before implementation.**

Run:

```bash
node deployment/compose/compose.e2e.contract.test.mjs
```

Expected: FAIL because the draft starts the application on the runner instead of in Compose.

- [x] **Step 3: Implement the container runtime overlay.**

Add service environment values for:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/emme
SPRING_DATA_REDIS_HOST=redis
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://keycloak:8080/realms/emme
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://keycloak:8080/realms/emme/protocol/openid-connect/certs
APP_KEYCLOAK_BASE_URL=http://keycloak:8080
APP_KEYCLOAK_ISSUER_URI=http://keycloak:8080/realms/emme
```

The workflow must explicitly start the platform service after migrations and provisioning. The overlay must not define a second host-process path.

- [x] **Step 4: Make provisioning idempotent and safe.**

Create `tools:e2e-provisioner` as a short-lived Java application using Spring JDBC and the existing generic throwable connection executor. It must generate the Keycloak realm document through typed Jackson nodes, call the Keycloak Admin API through an injected HTTP adapter, and perform idempotent prepared-statement tenant/membership seeding through application-owned interfaces. Passwords must never be logged. The workflow may retain a small readiness loop, but it must not execute dynamic realm or SQL seed shell scripts.

- [x] **Step 5: Run the contract and provisioning-tool checks.**

Run:

```bash
node deployment/compose/compose.e2e.contract.test.mjs
./gradlew :tools:e2e-provisioner:test --no-daemon --no-configuration-cache
docker compose -f deployment/compose/compose.yml -f deployment/compose/compose.jvm.yml -f deployment/compose/compose.e2e.yml config --quiet
```

Expected: PASS.

- [ ] **Step 6: Commit.**

```bash
git add deployment/compose tools settings.gradle.kts
git commit -m "feat(e2e): add typed service provisioning tool"
```

### Task 3: Make image creation reproducible for JVM and Native variants

**Files:**
- Modify: `applications/emme-platform/build.gradle.kts`
- Modify: `build-logic/src/main/kotlin/com/emme/buildlogic/container/EmmeContainerExtension.kt`
- Modify: `build-logic/src/main/kotlin/com/emme/buildlogic/container/EmmeContainerPlugin.kt`
- Modify: `build-logic/src/main/kotlin/com/emme/buildlogic/container/task/BuildContainerImageTask.kt`
- Create: `deployment/docker/Dockerfile.jvm`
- Create: `deployment/docker/Dockerfile.native`
- Test: `build-logic/src/test/kotlin/com/emme/buildlogic/ContainerImageConfigurationTest.kt`

**Interfaces:**
- `containerBuild` receives `emme.container.imageName`, `emme.container.imageTags`, and `emme.container.dockerfile` lazily.
- `containerVerify` scans the exact image tag selected by CI.
- JVM builds use the boot JAR; Native builds use the GraalVM native executable produced by the explicit Native task.

- [ ] **Step 1: Add failing build-logic configuration tests.**

Test that the container plugin exposes a Dockerfile property, preserves lazy image configuration, and uses the selected image tags when registering `containerBuild`.

- [ ] **Step 2: Run the focused build-logic test.**

```bash
./gradlew :build-logic:test --tests '*ContainerImageConfigurationTest' --no-daemon --no-configuration-cache
```

Expected: FAIL because Dockerfile and tag wiring are currently absent.

- [ ] **Step 3: Implement the minimum lazy configuration.**

Wire the extension’s Dockerfile and image-tags properties into `BuildContainerImageTask`. Keep provider selection lazy and do not instantiate Docker during Gradle configuration.

- [ ] **Step 4: Add non-root runtime Dockerfiles.**

The JVM image must copy the built `emme-platform` boot JAR into a minimal Java 25 runtime image and run as a non-root user. The Native image must copy the native executable into a minimal compatible runtime image and expose port `8081`.

- [ ] **Step 5: Run image and build-logic verification.**

```bash
./gradlew :build-logic:check :applications:emme-platform:bootJar --no-daemon --no-configuration-cache
./gradlew :applications:emme-platform:containerBuild -Pemme.container.imageName=emme-service:local-jvm -Pemme.container.dockerfile=deployment/docker/Dockerfile.jvm --no-daemon --no-configuration-cache
```

Expected: build-logic passes and the local JVM image is created when Docker is available.

- [ ] **Step 6: Commit.**

```bash
git add build-logic applications/emme-platform/build.gradle.kts deployment/docker
git commit -m "feat(container): add reproducible JVM and native image variants"
```

### Task 4: Add K3d local and K3s production deployment contracts

**Files:**
- Modify: `deployment/k3d/cluster.yml`
- Modify: `deployment/k3d/registry.yml`
- Modify: `deployment/scripts/bootstrap-local-registry.sh`
- Modify: `deployment/scripts/wait-for-cluster.sh`
- Modify: `mise.toml`
- Modify: `infra/kubernetes/overlays/k3d-jvm/kustomization.yaml`
- Modify: `infra/kubernetes/overlays/k3d-native/kustomization.yaml`
- Modify: `infra/kubernetes/overlays/k3s-production-jvm/kustomization.yaml`
- Modify: `infra/kubernetes/overlays/k3s-production-native/kustomization.yaml`
- Test: `.github/workflows/ci-backend.yml`

**Interfaces:**
- Local commands are exposed through `mise run k3d:bootstrap`, `mise run k3d:apply:jvm`, and `mise run k3d:apply:native`.
- Production receives an immutable `IMAGE_DIGEST` and never uses `latest`.

- [ ] **Step 1: Add manifest and command contract checks.**

Assert that each Kustomize overlay resolves the canonical service image, uses an explicit namespace, and contains no `latest` tag in production overlays.

- [ ] **Step 2: Implement thin K3d aliases.**

Keep registry creation and cluster bootstrap as thin wrappers. Replace generic `deploy-k3d.sh` with `mise` tasks delegating directly to `k3d` and `kubectl apply -k`. Do not put manifest content in scripts.

- [ ] **Step 3: Normalize production promotion.**

Add an Actions-ready image digest substitution contract. Production applies a Kustomize overlay only after the image has been built, scanned, published, and approved by the protected environment.

- [ ] **Step 4: Run infrastructure verification.**

```bash
kubectl kustomize infra/kubernetes/overlays/k3d-jvm >/dev/null
kubectl kustomize infra/kubernetes/overlays/k3d-native >/dev/null
kubectl kustomize infra/kubernetes/overlays/k3s-production-jvm >/dev/null
kubectl kustomize infra/kubernetes/overlays/k3s-production-native >/dev/null
```

Expected: PASS.

- [ ] **Step 5: Commit.**

```bash
git add deployment/k3d deployment/scripts mise.toml infra/kubernetes
git commit -m "feat(deployment): add explicit k3d and k3s runtime targets"
```

### Task 5: Add service image CI and deployment verification

**Files:**
- Create: `.github/workflows/container-image.yml`
- Modify: `.github/workflows/ci-backend.yml`
- Modify: `.github/workflows/ci-module-boundaries.yml`
- Modify: `tasks/todo.md`

**Interfaces:**
- Pull requests validate image creation and scanning without publishing.
- `main` and release workflows publish immutable JVM and optional Native images to GHCR.
- The workflow exports `EMME_SERVICE_IMAGE` for the web repository’s dispatchable E2E workflow.

- [ ] **Step 1: Add workflow contract checks.**

Validate with actionlint/YAML parsing that PRs build and scan, while publishing requires `main` or an explicit release event. Confirm no workflow uses `latest` for deployment.

- [ ] **Step 2: Implement the image workflow.**

Use Java setup, Gradle caching, Docker Buildx, `containerBuild`, `containerVerify`, OCI metadata, and GHCR login only on protected publish events. Use `sha-${GITHUB_SHA}` tags and upload scan reports.

- [ ] **Step 3: Run local workflow validation.**

```bash
actionlint .github/workflows/container-image.yml .github/workflows/ci-backend.yml
docker compose -f deployment/compose/compose.yaml -f deployment/compose/compose.runtime-jvm.yaml config --quiet
```

- [ ] **Step 4: Commit.**

```bash
git add .github/workflows tasks/todo.md
git commit -m "ci(container): build and verify immutable service images"
```

### Task 6: Run the complete service verification

**Files:**
- Modify: `docs/superpowers/plans/2026-08-03-service-runtime-deployment.md`
- Modify: `tasks/todo.md`

- [ ] **Step 1: Run service formatting and tests.**

```bash
./gradlew spotlessCheck test :build-logic:check --no-daemon --no-configuration-cache
```

- [ ] **Step 2: Run documentation, deployment, shell, and manifest checks.**

```bash
node scripts/validate-markdown.mjs
bash -n database/docker/run-migrations.sh deployment/scripts/*.sh infra/keycloak/*.sh
docker compose -f deployment/compose/compose.yaml -f deployment/compose/compose.runtime-jvm.yaml config --quiet
```

- [ ] **Step 3: Record evidence and push.**

Update this plan and `tasks/todo.md` with exact command results, commit each logical slice, push `feat/enterprise-module-template-conformance`, and verify the remote head.
