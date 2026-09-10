# K3s Helm Readiness Audit Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to execute remediation tasks after this audit is approved. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish an evidence-backed inventory of the current k3s, Helm, GitHub Actions, Kustomize, secrets, rollback, and observability implementation before changing the deployment path.

**Architecture:** The target baseline is a single-node, low-cost k3s cluster using GitHub Actions to build and scan immutable images, Helm to package and release the application, and k3s/Traefik for ingress. Argo CD, Argo Rollouts, Istio, and Envoy sidecars are deferred unless the audit finds a concrete operational requirement. Existing Kustomize output remains the comparison and rollback baseline until Helm parity is proven.

**Tech Stack:** k3s, Kubernetes, Helm 3, Kustomize, GitHub Actions, GHCR, Traefik, Terraform/cloud-init, Spring Boot Actuator, PostgreSQL, Redis, and the repository's existing deployment validation scripts.

## Global Constraints

- This plan is read-only until a separate remediation plan is approved.
- Do not run `helm install`, `helm upgrade`, `kubectl apply`, `kubectl delete`, or any command that changes cluster state during the audit.
- Do not expose, copy, or commit kubeconfigs, tokens, private keys, passwords, or rendered secret values.
- Preserve unrelated uncommitted work in the caller's worktree.
- Classify every capability as `Done`, `Partial`, `Missing`, `Blocked`, or `Deferred` and attach command output, file evidence, or an explicit external dependency.
- Production image references must be immutable digests or release-specific immutable tags; `latest` is a finding, not an accepted state.
- The backend contract remains port `8081` with Actuator health endpoints under `/actuator/health`.
- The public entry point remains the frontend/ingress path; the backend must remain internal unless an existing approved contract says otherwise.
- Any future controller or operator requires a version, namespace, resource budget, health check, upgrade path, and recovery path before selection.

## Audit Status Model

| Status | Meaning | Required evidence |
|---|---|---|
| `Done` | Implemented and verified against the target contract. | Passing command/test output and source location. |
| `Partial` | Some implementation exists, but an acceptance condition is absent or unverified. | Existing evidence plus explicit gap. |
| `Missing` | No usable implementation found. | Search result and expected artifact/behavior. |
| `Blocked` | Verification needs access, credentials, a live cluster, or an external owner. | Exact dependency and safe reproduction command. |
| `Deferred` | Intentionally excluded from the single-node baseline. | Decision rationale and trigger for reconsideration. |

## Expected Audit Deliverables

- `docs/superpowers/reviews/2026-09-09-k3s-helm-readiness-audit.md` — evidence matrix and findings.
- `docs/superpowers/reviews/2026-09-09-k3s-helm-readiness-gaps.md` — ordered remediation backlog, if gaps exist.
- Updated deployment documentation only when the audit finds an existing document that contradicts verified behavior; no speculative architecture edits.
- A recommendation: retain current Kustomize deployment, cut over to Helm, or run a staged Helm parity migration.
- An explicit decision on whether Argo CD, Argo Rollouts, Istio, or Envoy sidecars remain deferred.

## File and Boundary Map

### CI/CD

- `.github/workflows/ci-backend.yml` — quality, tests, infrastructure rendering, and security gates.
- `.github/workflows/container-image.yml` — image build, scan, push, and digest publication.
- `.github/workflows/ci-doctor-smoke.yml` — post-CI production smoke checks.
- `.github/actions/setup-gradle/action.yml` — shared build setup.
- `scripts/validate-backend-workflow.mjs` — backend workflow contract checks.
- `scripts/validate-container-workflow.mjs` — image workflow contract checks.

### Helm and Kubernetes

- `deployment/helm/emme/Chart.yaml` — application chart metadata.
- `deployment/helm/emme/values.yaml` — chart defaults and image/resource/probe configuration.
- `deployment/helm/emme/templates/_helpers.tpl` — chart names and labels.
- `deployment/helm/emme/templates/deployment.yaml` — workload contract.
- `deployment/helm/emme/templates/service.yaml` — service contract.
- `infra/kubernetes/base/` — Kustomize comparison baseline.
- `infra/kubernetes/overlays/` — environment/runtime overlays; record the exact directories present rather than assuming names from older docs.
- `deployment/k3d/` — local Kubernetes configuration.
- `infra/k3s/` — k3s server/agent/add-on configuration when present.
- `infra/terraform/` — VM, cloud-init, firewall, and k3s bootstrap source.

### Runtime and operations

- `docs/architecture/04-delivery/` — delivery, deployment, release, rollout, and rollback contracts.
- `docs/architecture/05-operations/` — production readiness and recovery contracts.
- `docs/superpowers/specs/` and `docs/superpowers/plans/` — prior approved decisions and known gaps.
- `scripts/validate-deployment-contracts.mjs` and `scripts/validate-deployment-contracts.test.mjs` — existing deployment checks.

## Task 1: Establish a safe audit baseline

**Files:**
- Read: repository status, branch, recent commits, remotes, and existing uncommitted files.
- Create: `docs/superpowers/reviews/2026-09-09-k3s-helm-readiness-audit.md`.

- [ ] Record the branch and commit being audited.
- [ ] Record unrelated modified and untracked files without staging or altering them.
- [ ] Record whether `helm`, `kubectl`, `kustomize`, `terraform`, `docker`, and `gh` are installed, including versions.
- [ ] Record whether a live kube context is configured, but do not print credentials or full kubeconfig contents.
- [ ] Record the exact cluster context name, server version, node count, and node roles only if a safe read-only context exists.
- [ ] Add an audit header with timestamp, commit SHA, tool versions, and live-cluster availability.

Run:

```bash
git status --short --untracked-files=all
git branch --show-current
git rev-parse HEAD
helm version --short
kubectl version --client
kustomize version
terraform version
docker version --format '{{.Server.Version}}' 2>/dev/null || true
gh --version
kubectl config current-context 2>/dev/null || true
kubectl cluster-info 2>/dev/null || true
```

Expected result: the audit records what can be verified locally and marks unavailable live-cluster evidence as `Blocked`, without treating lack of cluster access as proof that a feature is absent.

## Task 2: Inventory current deployment sources

**Files:**
- Read: `deployment/helm/emme/**`.
- Read: `infra/kubernetes/base/**` and every directory under `infra/kubernetes/overlays/`.
- Read: `deployment/k3d/**`, `infra/k3s/**`, and `infra/terraform/**`.
- Read: relevant deployment specifications and plans under `docs/superpowers/`.
- Update: the audit evidence matrix only.

- [ ] List every Helm template, values file, dependency, hook, and chart version.
- [ ] List every Kustomize resource, patch, image override, namespace, service, ingress, HPA, Secret reference, ConfigMap, and migration job.
- [ ] Identify the authoritative source for each workload and record conflicts where Helm and Kustomize both define it.
- [ ] Inspect bootstrap configuration for disabled packaged components; do not assume repository prose matches cloud-init behavior.
- [ ] Identify whether Traefik is bundled, disabled, separately installed, or unknown.
- [ ] Identify storage classes, persistent volume claims, backup jobs, and external object-storage references.
- [ ] Identify all files mentioning Argo CD, Argo Rollouts, Istio, Envoy, Flux, cert-manager, External Secrets, Longhorn, Velero, CloudNativePG, or Redis operators.

Expected result: a source-of-truth map with no unclassified deployment resource.

## Task 3: Verify the application Helm chart against the Kustomize contract

**Files:**
- Read: `deployment/helm/emme/Chart.yaml`.
- Read: `deployment/helm/emme/values.yaml`.
- Read: `deployment/helm/emme/templates/_helpers.tpl`.
- Read: `deployment/helm/emme/templates/deployment.yaml`.
- Read: `deployment/helm/emme/templates/service.yaml`.
- Compare: `infra/kubernetes/base/backend-deployment.yaml`, `backend-service.yaml`, `backend-hpa.yaml`, `namespace.yaml`, and relevant overlays.
- Update: the audit evidence matrix only.

- [ ] Run `helm lint deployment/helm/emme`.
- [ ] Run `helm template emme deployment/helm/emme` and save output outside the repository.
- [ ] Check rendered namespace, labels, backend port `8081`, service port, health probes, startup behavior, resource requests/limits, security context, termination policy, replica count, HPA behavior, and image pull policy.
- [ ] Check whether the chart renders frontend, ingress, TLS, migration jobs, ConfigMaps, NetworkPolicies, or Secret references required by the current Kubernetes path.
- [ ] Check whether values named `postgresql` and `redis` have actual chart dependencies/templates or are unused configuration.
- [ ] Check for mutable `latest` references and image digest support.
- [ ] Produce a mismatch table with columns: contract, Kustomize behavior, Helm behavior, status, risk, and remediation task.

Known starting hypotheses to verify, not assume:

- The chart appears to render only one application Deployment and one Service.
- The chart defaults to `latest`.
- The chart values mention PostgreSQL and Redis without necessarily rendering them.
- The chart may not yet be production-parity with the Kustomize frontend/backend/ingress path.

Expected result: a precise Helm parity scorecard, not a general statement that “Helm exists.”

## Task 4: Verify CI image, promotion, and deployment behavior

**Files:**
- Read: `.github/workflows/ci-backend.yml`.
- Read: `.github/workflows/container-image.yml`.
- Read: `.github/workflows/ci-doctor-smoke.yml`.
- Read: `.github/workflows/security-scan.yml` and `.github/workflows/dependency-review.yml`.
- Read: `scripts/validate-backend-workflow.mjs` and `scripts/validate-container-workflow.mjs`.
- Update: the audit evidence matrix only.

- [ ] Verify CI runs unit, integration, architecture, infrastructure-render, and security checks.
- [ ] Verify the image workflow publishes immutable identifiers and exposes the digest needed for deployment.
- [ ] Verify whether frontend and backend images are both built and promoted as one release bundle.
- [ ] Verify whether Helm lint, Helm template, Kubernetes schema validation, and rendered-secret checks run in CI.
- [ ] Verify whether any workflow performs `helm upgrade --install`, `kubectl apply`, or protected deployment promotion.
- [ ] Verify whether smoke checks execute after an actual deployment or only after an externally managed deployment.
- [ ] Verify environment protection, least-privilege permissions, kubeconfig/OIDC handling, approval gates, timeout, `--atomic`, `--wait`, rollout status, and failure cleanup.
- [ ] Verify rollback uses a known-good image digest and Helm revision rather than a mutable tag.

Expected result: a CI/CD matrix separating build/scan, release metadata, deployment, post-deployment verification, and rollback. A smoke test without a repository-owned deployment path is `Partial`, not `Done`.

## Task 5: Verify k3s platform and resource budget

**Files:**
- Read: `infra/terraform/main.tf`.
- Read: `infra/terraform/variables.tf`.
- Read: `infra/terraform/cloud-init.yaml`.
- Read: `deployment/k3d/**`, `infra/k3s/**`, and documented topology/runbooks.
- Update: the audit evidence matrix only.

- [ ] Verify k3s version pinning, server/agent topology, registration address, cluster token handling, secrets encryption, firewall rules, and upgrade/rollback procedure.
- [ ] Verify whether cloud-init disables Traefik or relies on the bundled instance; reconcile this with ingress documentation.
- [ ] Verify node CPU, memory, disk, storage class, and allocatable capacity when live read-only access exists.
- [ ] Inventory all always-on workloads, including application, databases, Redis, identity, messaging, observability, ingress, and controllers.
- [ ] Estimate requested and limited CPU/memory against node allocatable capacity.
- [ ] Check whether two application replicas, local-path storage, and stateful workloads are realistic on one node.
- [ ] Check backup of k3s datastore/snapshots and server token, plus off-node storage destination.
- [ ] Mark HA claims as unsupported for a single-node cluster unless the documentation explicitly limits them.

Expected result: resource and failure-mode evidence showing whether the baseline fits the VPS and which claims require a larger cluster.

## Task 6: Verify secrets, stateful services, ingress, and recovery

**Files:**
- Read: Kubernetes Secret references and ConfigMaps under `infra/kubernetes/`.
- Read: Compose/Kubernetes PostgreSQL and Redis definitions.
- Read: `docs/security.md`, `docs/architecture/05-operations/**`, and deployment runbooks.
- Read: repository history only for secret names and contract changes; never print secret values.
- Update: the audit evidence matrix only.

- [ ] Identify the source of runtime secrets: CI environment, Bitwarden, Kubernetes Secret, External Secrets, or another provider.
- [ ] Confirm no literal credentials, placeholder secrets, or empty production secret values are committed.
- [ ] Confirm secret rotation triggers controlled workload restart where startup-loaded values require it.
- [ ] Identify PostgreSQL ownership, migration execution, backup format, restore target, measured RPO/RTO, and rollback constraints.
- [ ] Identify Redis persistence/recovery expectations and state clearly that one node is not HA.
- [ ] Verify ingress hostname/TLS ownership, frontend-to-backend routing, OAuth callbacks, SSE timeouts, and backend public exposure.
- [ ] Verify logs, metrics, alerts, and dashboards are available without requiring Istio telemetry.
- [ ] Mark backup claims as `Partial` until restore evidence exists.

Expected result: a data and access-risk table with a concrete recovery gap for every stateful component.

## Task 7: Audit the deferred Argo/Istio decision

**Files:**
- Read: all repository matches for Argo CD, Argo Rollouts, Istio, Envoy, and Flux.
- Read: official documentation for the versions considered during the audit.
- Update: the audit evidence matrix and recommendation section only.

- [ ] Confirm whether any CRDs, namespaces, controllers, webhooks, sidecar injection labels, gateways, VirtualServices, DestinationRules, Rollouts, or Applications exist.
- [ ] Confirm whether the current single-node release needs drift reconciliation beyond a protected GitHub Actions deployment.
- [ ] Confirm whether ordinary rolling updates and Helm rollback satisfy release risk.
- [ ] Confirm whether canary traffic splitting is required; if yes, evaluate Traefik or Gateway API before Istio.
- [ ] Confirm whether mTLS, mesh authorization, header routing, mirroring, or service-to-service telemetry is a current requirement.
- [ ] Record Argo CD as `Deferred` unless continuous reconciliation, multi-cluster management, or team self-service is a demonstrated need.
- [ ] Record Argo Rollouts as `Deferred` unless canary/blue-green traffic control is a demonstrated need.
- [ ] Record Istio/Envoy as `Deferred` unless mesh-specific traffic/security requirements outweigh sidecar and control-plane cost.

Expected result: a decision record with explicit triggers for reconsideration, not a blanket “never use” rule.

## Task 8: Produce findings, remediation order, and release recommendation

**Files:**
- Modify: `docs/superpowers/reviews/2026-09-09-k3s-helm-readiness-audit.md`.
- Create: `docs/superpowers/reviews/2026-09-09-k3s-helm-readiness-gaps.md` only if findings require follow-up work.

- [ ] Summarize each capability in a table: capability, current evidence, status, risk, owner, and next action.
- [ ] Separate immediate blockers from hardening work and deferred options.
- [ ] Order remediation by dependency: source-of-truth decision, Helm parity, immutable release contract, secrets, ingress, stateful recovery, CI deployment, then optional controllers.
- [ ] Define the minimum release gate for a single-node VPS: rendered chart validation, immutable image, secret contract, readiness/smoke checks, resource budget, backup evidence, and tested rollback.
- [ ] Choose one recommendation: retain Kustomize temporarily, staged Helm cutover, or no-go pending external evidence.
- [ ] State whether the audit supports adding Argo CD, Argo Rollouts, Istio, or Envoy sidecars now; default is deferred.
- [ ] Add exact verification commands and links to captured evidence without embedding secrets.
- [ ] Review the document for placeholders, contradictions, unsupported claims, and scope creep.

## Definition of Done

- [ ] Every deployment capability is classified with evidence.
- [ ] Helm and Kustomize responsibilities are explicitly compared.
- [ ] CI build, image publication, deployment, smoke, and rollback responsibilities are separated.
- [ ] k3s bootstrap behavior matches the documented ingress/storage topology.
- [ ] Single-node CPU, memory, disk, storage, and controller overhead are measured or explicitly blocked on live access.
- [ ] Secrets are traced from source to namespace without exposing values.
- [ ] PostgreSQL and Redis ownership and recovery expectations are documented.
- [ ] Argo CD, Argo Rollouts, Istio, and Envoy sidecars have a documented defer/adopt decision.
- [ ] No cluster-mutating command was run during the audit.
- [ ] The final audit and remediation backlog are committed and pushed to the feature branch.

## Recommended Audit Command Set

```bash
helm lint deployment/helm/emme
helm template emme deployment/helm/emme > /private/tmp/emme-current-helm.yaml
kubectl kustomize infra/kubernetes/overlays/k3d-jvm > /private/tmp/emme-k3d-jvm.yaml
kubectl kustomize infra/kubernetes/overlays/k3d-native > /private/tmp/emme-k3d-native.yaml
kubectl kustomize infra/kubernetes/overlays/k3s-production-jvm > /private/tmp/emme-k3s-jvm.yaml
kubectl kustomize infra/kubernetes/overlays/k3s-production-native > /private/tmp/emme-k3s-native.yaml
node scripts/validate-deployment-contracts.mjs
node scripts/validate-backend-workflow.mjs
node scripts/validate-container-workflow.mjs
kubectl get nodes -o wide
kubectl get pods -A
kubectl get storageclass
kubectl get ingress -A
kubectl get crd
kubectl top nodes
kubectl top pods -A
```

The live-cluster commands are read-only but may be unavailable. If a command fails because no context exists, record the exact error category and continue with repository evidence.
