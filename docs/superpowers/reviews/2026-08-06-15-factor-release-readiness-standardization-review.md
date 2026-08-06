# 15-Factor Release Readiness Standardization Review

| Field | Detail |
|---|---|
| Date | 2026-08-06 |
| Reviewed design | [`2026-08-06-15-factor-release-readiness-design.md`](../specs/2026-08-06-15-factor-release-readiness-design.md) |
| Repositories | `emme-service`, `emme-web` |
| Recommendation | Needs revision before implementation planning |
| Review scope | Naming, toolchains, deployment contracts, secrets, proxying, release evidence, and operational consistency |

## 1. Summary

The release-readiness design covers the correct areas, but the current repository and design use several competing conventions. These must be normalized before implementation begins so that Gradle, mise, CI, Compose, Kustomize, Kubernetes namespaces, secret projects, image promotion, and frontend routing all resolve the same environment contract.

The review found High and Medium findings. The design should be revised before the implementation plan is created.

## 2. Canonical Standard to Adopt

The environment identifier must flow unchanged through the entire release system:

```text
EnvironmentName
  → gradle/environments/<environment>.properties
  → deployment overlay
  → Kubernetes namespace / Compose project
  → secret project and Secret contract
  → image promotion metadata
  → CI environment
  → public hostname
```

The only canonical environment identifiers are:

```text
local
dev
regression
staging
prod
```

Legacy names such as `production`, `test`, `ci`, `k3d-*`, and `k3s-production-*` must either be removed or explicitly isolated to a temporary migration adapter with a removal task and deadline. Because the service is unreleased, the preferred standard is to remove obsolete names rather than retain indefinite compatibility aliases.

## 3. Findings

### F-01 — High: Environment names are not canonical in the implementation

The design uses `prod`, but the implementation still contains:

- `EnvironmentName.PRODUCTION("production")`
- `gradle/environments/production.properties`
- `VerifyEnvironmentTask` accepting `production` but not `prod`
- `KubernetesDeploymentTarget` resolving to `k3d-*` and `k3s-production-*`
- `gradle/secrets/manifest.json` using `production`

Required correction:

- Rename the enum value and property file to `prod`.
- Make `dev-*`, `staging-*`, and `prod-*` the canonical runtime overlay names.
- Update namespace and secret project resolution to use the same identifier.
- Remove obsolete aliases unless a documented migration consumer requires them.

### F-02 — High: Java 25 and GraalVM 25 toolchain selection is incomplete

Java 25 compilation is already declared in Gradle, but the active Gradle runtime can still be Java 17. Native Image is opt-in, but the design does not yet specify how the native launcher is selected or validated.

Required correction:

- Use Java 25 Temurin as the standard Gradle/JVM build runtime.
- Use an explicit GraalVM Community 25 installation for native compilation.
- Define the local `mise` setup and CI setup for both toolchains.
- Validate `JAVA_HOME`, `GRAALVM_HOME`, `java -version`, and `native-image --version`.
- Keep native fallback disabled.
- Do not depend on ambiguous automatic GraalVM toolchain detection.

GraalVM’s Native Build Tools documentation requires a GraalVM JDK for native builds and documents limitations around automatic toolchain detection. The implementation must make this selection explicit.

### F-03 — High: Kubernetes Secret contract is underspecified

The design mentions Bitwarden and GitHub Actions but does not define the exact runtime contract or materialization flow.

Required correction:

- Define exact Secret names and keys.
- Define the provider input contract for Bitwarden and GitHub Actions.
- Define the validation command executed before deployment.
- Materialize values as namespaced Kubernetes Secrets without committing rendered values.
- Define rotation behavior and rollout triggering.
- Rename the `production` secret manifest section to `prod`.
- Fail closed when a required key or provider is missing.

The base backend manifest must contain no empty, placeholder, or literal credential values. Workloads must use `secretKeyRef` or a reviewed `envFrom` contract.

### F-04 — High: Image immutability is not enforced for both applications

The design requires immutable promotion, but current deployment definitions still use mutable tags and the frontend image is not consistently included in release promotion.

Required correction:

- Require backend and frontend image digests for staging and production.
- Reject `latest` and mutable release tags in Kustomize validation.
- Generate one release manifest containing backend digest, frontend digest, source SHAs, API contract version, and release identifier.
- Promote the backend/frontend pair together.
- Record digests in deployment evidence and rollback metadata.

### F-05 — High: Cross-repository release pairing is undefined

The backend is built in `emme-service` while the frontend is built in `emme-web`, but no artifact binds the two images into one release.

Required correction:

Create a release bundle such as:

```yaml
release: 2026.08.06-rc.1
apiContract: 1.0
backend:
  image: ghcr.io/migangdelzar/emme-service
  digest: sha256:...
  sourceSha: ...
frontend:
  image: ghcr.io/migangdelzar/emme-web
  digest: sha256:...
  sourceSha: ...
```

Promotion, staging, production, and rollback must consume this pair rather than independently selecting image tags.

### F-06 — Medium: Compose and Kubernetes service names differ

Compose currently uses the service name `emme-platform`, while Kubernetes uses `backend`. The release design claims parity and the frontend Kubernetes proxy uses `backend:8081`.

Required correction:

- Standardize the runtime service name to `backend` across Compose and Kubernetes.
- Keep `ghcr.io/migangdelzar/emme-service` as the image name.
- Update mise tasks, E2E provisioning, health checks, and Compose references together.

### F-07 — Medium: Legacy names are embedded in validators and workflows

The following still reference old file and overlay names:

- `mise.toml`
- `.github/workflows/ci-backend.yml`
- `scripts/validate-emme-platform-target.mjs`
- `scripts/validate-emme-platform-target.test.mjs`
- Compose contract tests
- deployment architecture documentation

Required correction:

Update validators and workflows in the same logical migration as the file renames. A rename is incomplete while a validator still requires the old path.

### F-08 — Medium: Verification command naming is inconsistent

The design references `environmentVerify`, but the build logic registers `verifyEnvironment`.

Required correction:

- Use `verifyEnvironment` consistently.
- Add one parameterized test for all five canonical environments.
- Add explicit checks that the selected target, runtime, overlay, namespace, and image policy agree.

### F-09 — Medium: Kustomize patching is brittle

Current production patches remove environment entries by numeric array position. Adding or reordering one base variable can silently patch the wrong value.

Required correction:

- Use named strategic merge patches or a stable Secret/ConfigMap contract.
- Avoid JSON patches that depend on `/env/0`, `/env/1`, and similar positions.
- Render every overlay in CI after base changes.

### F-10 — Medium: Ingress and TLS contract lacks concrete standards

The design requires Ingress but does not specify its controller, class, hostname, TLS issuer, or environment behavior.

Required correction:

- Standardize on `networking.k8s.io/v1`.
- Define `ingressClassName` explicitly.
- Define hostnames for `dev`, `staging`, and `prod`.
- Define TLS behavior and certificate issuer per environment.
- Route the public host to `frontend:80` only.
- Keep `backend:8081` cluster-internal.

### F-11 — Medium: The 15 factors lack uniform evidence metadata

The design lists changes and evidence, but not a consistent owner, command, artifact, blocking status, or threshold.

Required correction:

Add this structure to every factor:

| Field | Required value |
|---|---|
| Owner | Service, web, CI, platform, or release |
| Change set | Exact files or workflow |
| Command | Reproducible verification command |
| Evidence | Log, report, artifact, or deployment record |
| Threshold | Pass/fail value where measurable |
| Blocking | Whether promotion stops on failure |

### F-12 — Medium: Security defaults remain in disposable environments

Current regression Compose configuration contains fallback credentials and literal encryption values. Even if disposable, these values can leak into logs, recordings, or copied environments.

Required correction:

- Require explicit regression secret inputs.
- Keep only clearly generated, scoped test credentials in ignored local files or CI secret stores.
- Add secret scanning to Compose and Kubernetes validation.
- Never use production-like credentials in examples.

### F-13 — Medium: The design duplicates derived deployment data

Environment properties contain `kustomize.overlay`, while the deployment provider also derives an overlay from environment and runtime.

Required correction:

- Make environment and runtime the inputs.
- Derive overlay, namespace, Compose file, and image policy from one resolver.
- Remove duplicated `kustomize.overlay` values unless they are validated against the resolver output.

### F-14 — Low: “15-factor” terminology is ambiguous

The project checklist is a local release-control model, not a universally defined standard.

Required correction:

Use the name **EMME 15-Factor Release Controls** throughout the design, plan, CI checks, and release evidence. This makes the project-specific scope explicit.

## 4. Recommended Standardization Sequence

1. Canonicalize `prod` and remove obsolete environment names.
2. Make Java 25 and GraalVM 25 toolchain selection explicit.
3. Define the exact Kubernetes Secret and provider contract.
4. Standardize backend service name as `backend`.
5. Rename Compose/Kustomize files and update all validators/workflows.
6. Remove positional Kustomize patches.
7. Add the backend/frontend release bundle.
8. Define Ingress, TLS, namespace, and public-host contracts.
9. Add uniform evidence metadata to all 15 controls.
10. Enforce immutable images and secret scanning.

## 5. Recommendation

**Needs revision before implementation planning.** Findings F-01 through F-05 are release-blocking because they can produce inconsistent environments, non-reproducible artifacts, or unsafe secret handling. Findings F-06 through F-13 should be addressed in the same design revision to keep the deployment contract standardized rather than layering compatibility exceptions.

