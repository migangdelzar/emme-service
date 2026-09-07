# K3s Single-Node Helm and Operator Strategy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the single-node K3s deployment repeatable with Helm while adding only the operators that provide clear operational value for a small VPS.

**Architecture:** Helm will package the EMME application and install selected third-party charts. The existing Kustomize overlays remain the comparison baseline until rendered Helm output reaches parity; there is no big-bang migration. Plain Helm driven by CI or an operator-free release script is the default control plane for this small cluster. GitOps and stateful-data operators are optional follow-up decisions, not prerequisites.

**Tech Stack:** K3s, Helm 3, Kubernetes `apps/v1`/`networking.k8s.io/v1`, the existing Spring Boot application, Kustomize baseline overlays, the K3s-bundled Traefik ingress, and only officially supported upstream/community charts selected after version research.

## Global Constraints

- Target is a single-node or small low-cost VPS K3s cluster.
- Do not add an operator merely because it is popular; every controller must have a documented failure-mode and resource-budget justification.
- Do not run duplicate ingress, storage, metrics-server, or secret controllers when K3s already provides the capability.
- Do not commit secret values, kubeconfigs, private keys, or rendered secret manifests.
- Replace mutable image tags such as `latest` with immutable release tags or digests before production use.
- Preserve the current backend contract: service port `8081`, liveness/readiness endpoints under `/actuator/health`, and the `emme-prod` namespace for production.
- Keep the current Kustomize output as a rollback/comparison baseline until Helm parity is verified.
- Do not run `helm install`, `kubectl apply`, or cluster-wide changes until rendered manifests pass validation and the operator selection is approved.
- Production readiness requires measured backup/restore evidence, not only a successful backup command.

---

## 1. Current Repository Context

The next session must begin from these facts:

- Application delivery currently lives in `infra/kubernetes/base` and `infra/kubernetes/overlays/{dev,prod}`.
- The existing chart is `deployment/helm/emme`, but it is not production-parity: it defaults to two replicas, uses `:latest`, exposes port `80` while targeting `8081`, and declares PostgreSQL/Redis values without chart dependencies or templates.
- The production Kustomize overlay currently sets the backend to two replicas and image tag `0.1.0` in namespace `emme-prod`.
- The repository documentation currently treats Kustomize as authoritative for application environment overlays and Helm as primarily a third-party installation mechanism.
- K3s already bundles Traefik and a local-path storage provisioner in the usual installation profile; verify the exact installed components before adding replacements.
- The application exposes Prometheus-compatible metrics through Spring Boot Actuator, and Kubernetes alert/dashboard manifests already exist in `infra/kubernetes/base`.
- Existing operational policy requires encrypted, access-controlled, monitored, and restorable backups; the data-lifecycle document requires a restore exercise with measured RPO/RTO.

## 2. Operator Recommendation to Validate

Use this as the starting hypothesis, not as permission to install everything:

| Component | Recommendation for single-node K3s | Reason | Default action |
|---|---|---|---|
| Helm | Yes | Packages the application and third-party releases without another always-on controller. | Adopt as the application packaging format. |
| Traefik | Keep K3s-bundled instance | Avoids a duplicate ingress controller and preserves the current K3s path. | Reuse; configure only through the chosen supported mechanism. |
| `cert-manager` | Conditional yes | Strong value if public HTTPS certificates are not already managed elsewhere. | Install only if TLS is cluster-managed. |
| External Secrets Operator | Conditional yes | Useful if Bitwarden, Vault, or another external secret manager is the source of truth. | Install only after the provider and recovery path are selected. |
| CloudNativePG | Conditional later | Good lifecycle and backup primitives for self-hosted PostgreSQL, but adds controller and storage complexity. | Evaluate only if PostgreSQL must be self-hosted in K3s. |
| Prometheus Operator / `kube-prometheus-stack` | Conditional later | Powerful dashboards and alerts, but potentially heavy for a small VPS and overlaps with existing application metrics. | Measure resource budget first; do not install by default. |
| Longhorn | No by default | Replicated storage has little value on one node and adds substantial overhead. | Keep local-path storage plus tested off-cluster backups. |
| Redis operator | No by default | Redis lifecycle automation is not justified until HA/persistence requirements exist. | Use an explicit Redis deployment or managed Redis contract. |
| Argo CD or Flux | No initially | An always-on GitOps controller is extra control-plane complexity for a single node. | Use Helm from CI first; revisit only if drift reconciliation is a real need. |
| Velero | Conditional later | Useful for cluster-object backup, but it does not replace database-native backups and requires object storage. | Evaluate after database backup design. |

The plan must confirm current chart compatibility, supported Kubernetes versions, CRD installation behavior, upgrade policy, resource requests, and uninstall/recovery behavior from official documentation before selecting versions.

## 3. File and Boundary Map

### Existing files to inspect and likely modify

- `deployment/helm/emme/Chart.yaml` — chart metadata, dependencies only if explicitly justified.
- `deployment/helm/emme/values.yaml` — safe defaults, image digest/tag, environment, resources, probes, service, ingress, and secret references.
- `deployment/helm/emme/templates/_helpers.tpl` — stable labels and names matching the current Kubernetes contracts.
- `deployment/helm/emme/templates/deployment.yaml` — parity with the backend Deployment, security context, probes, resources, and secret references.
- `deployment/helm/emme/templates/service.yaml` — preserve the backend service contract.
- `infra/kubernetes/base/backend-deployment.yaml` — parity source during migration.
- `infra/kubernetes/base/backend-service.yaml` — service and port contract.
- `infra/kubernetes/base/backend-hpa.yaml` — determine whether HPA is meaningful on one node or should be disabled by default.
- `infra/kubernetes/overlays/dev/kustomization.yaml` and `infra/kubernetes/overlays/prod/kustomization.yaml` — render comparison fixtures and rollback path.
- `README.md` and `docs/architecture/04-delivery/deployment.md` — document the final Helm/Kustomize ownership boundary and commands.

### New files to create only when the corresponding decision is approved

- `deployment/helm/emme/values-dev.yaml` — non-secret development overrides.
- `deployment/helm/emme/values-prod.yaml` — production-safe non-secret overrides.
- `deployment/helm/platform/` or an equivalent documented release manifest — third-party chart release definitions, kept separate from the application chart.
- `scripts/validate-helm-release.mjs` — deterministic chart rendering and contract checks, if the existing deployment validator cannot be extended safely.
- `docs/architecture/04-delivery/k3s-helm-operations.md` — operator selection, upgrade, rollback, backup, and recovery runbook.
- `docs/superpowers/specs/2026-09-07-k3s-helm-operator-strategy-design.md` — approved design produced before implementation if the new session identifies multiple independent subsystems.

Do not create a chart dependency for PostgreSQL or Redis merely because the current values file names those services. First establish whether those databases are cluster-owned, externally managed, or intentionally deferred.

## 4. Execution Tasks

### Task 1: Establish a clean baseline and deployment contract

**Files:**
- Read: `deployment/helm/emme/**`
- Read: `infra/kubernetes/base/**`
- Read: `infra/kubernetes/overlays/{dev,prod}/kustomization.yaml`
- Read: `scripts/validate-deployment-contracts.mjs` and existing deployment tests
- Modify: `tasks/todo.md` only if this feature becomes the active workstream

- [ ] Record the current branch, clean/dirty status, Kubernetes client/server versions, installed K3s addons, node allocatable CPU/memory/storage, and current namespaces.
- [ ] Render both Kustomize environments and record the expected backend image, replica, service, probes, resources, namespace, and secret-reference contracts.
- [ ] Render the current Helm chart and list every parity mismatch; do not fix mismatches in this task.
- [ ] Capture a rollback command using the current Kustomize overlay before changing cluster ownership.
- [ ] Commit only the baseline/contract documentation if a new artifact is created: `docs(k3s): record Helm migration baseline`.

Expected evidence:

```bash
kubectl kustomize infra/kubernetes/overlays/dev > /tmp/emme-dev-kustomize.yaml
kubectl kustomize infra/kubernetes/overlays/prod > /tmp/emme-prod-kustomize.yaml
helm lint deployment/helm/emme
helm template emme deployment/helm/emme > /tmp/emme-current-helm.yaml
```

### Task 2: Research supported charts and choose the minimum operator set

**Files:**
- Create: `docs/superpowers/specs/2026-09-07-k3s-helm-operator-strategy-design.md`
- Read: official documentation for Helm, K3s packaged components, cert-manager, External Secrets Operator, CloudNativePG, the selected observability chart, and the selected secret provider

- [ ] Verify current K3s bundled components and whether the cluster already has Traefik, local-path storage, and metrics-server.
- [ ] Compare Helm-only releases against Flux and Argo CD for a single-node cluster, including controller resource usage and recovery behavior.
- [ ] Check each candidate chart’s supported Kubernetes version range, CRD lifecycle, upgrade notes, namespace behavior, default resource requests, and uninstall limitations.
- [ ] Decide the TLS ownership model: K3s/Traefik with existing certificate automation, or cert-manager with a documented issuer and renewal path.
- [ ] Decide the secret ownership model: CI-created namespaced Secrets, External Secrets with a named provider, or another explicit mechanism. Never leave this as an implicit default.
- [ ] Decide whether PostgreSQL is cluster-owned. If yes, compare CloudNativePG against the current deployment and require a restore exercise before migration. If no, do not add a database operator.
- [ ] Decide whether observability can fit the node budget. Prefer the existing Actuator metrics and lightweight scraping unless full Kubernetes dashboards/alerts justify the Prometheus Operator footprint.
- [ ] Record the selected versions, resource budget, ownership boundary, rollback plan, and rejected alternatives in the design document.

Decision gate: the next task cannot install a CRD or operator until the design explicitly names the selected components and the reason each non-selected component is excluded.

### Task 3: Make the application chart production-parity

**Files:**
- Modify: `deployment/helm/emme/Chart.yaml`
- Modify: `deployment/helm/emme/values.yaml`
- Modify: `deployment/helm/emme/templates/_helpers.tpl`
- Modify: `deployment/helm/emme/templates/deployment.yaml`
- Modify: `deployment/helm/emme/templates/service.yaml`
- Create: `deployment/helm/emme/values-dev.yaml`
- Create: `deployment/helm/emme/values-prod.yaml`
- Test: existing deployment contract tests or `scripts/validate-helm-release.mjs`

- [ ] Add a failing render/contract test for the required namespace, backend service port `8081`, health probes, immutable image reference, non-root security context, and environment-specific replica/resource values.
- [ ] Run the focused test and confirm it fails against the current chart because of the known parity gaps.
- [ ] Update the chart with the minimum fields needed to match the Kustomize contract; do not add database subcharts or unrelated application features.
- [ ] Change defaults so an accidental production install cannot use `latest`, empty credentials, or a two-replica assumption that the node cannot satisfy.
- [ ] Keep secrets as `existingSecret`/secret-key references or external-secret references; do not add literal values to values files.
- [ ] Render dev and prod values, compare the output with the Kustomize baseline, and document intentional differences.
- [ ] Run Helm lint, template rendering, YAML/schema validation, and the existing deployment contract validator.
- [ ] Commit: `feat(helm): make EMME application chart production-parity`.

### Task 4: Add only the approved foundational chart releases

**Files:**
- Create or modify: `deployment/helm/platform/` release manifests selected in Task 2
- Create: chart-specific values files under `deployment/helm/platform/`
- Modify: deployment documentation and validation scripts

- [ ] Write a failing validation test that rejects unapproved CRDs/controllers, mutable images, missing namespaces, and missing resource requests/limits.
- [ ] Add the approved chart repositories and pinned chart versions through the repository’s chosen release mechanism.
- [ ] Install cert-manager only if cluster-managed TLS was approved; validate issuer readiness and certificate renewal behavior.
- [ ] Install External Secrets only if a provider, authentication path, and restore procedure were approved; validate that no secret value is rendered into Git or Helm output.
- [ ] Configure observability only to the approved footprint; do not install `kube-prometheus-stack` by default without a measured resource budget.
- [ ] Reuse K3s Traefik and local-path storage rather than deploying duplicate controllers.
- [ ] Run `helm template`, `kubectl diff --server-side --dry-run=server`, and CRD/controller health checks in a disposable or non-production namespace before production installation.
- [ ] Commit each independently deployable foundational release in its own logical commit.

### Task 5: Select and implement the release workflow

**Files:**
- Modify: `.github/workflows/container-image.yml` or the deployment workflow selected after inspection
- Create: a Helm release workflow/script if none exists
- Modify: `README.md` and `docs/architecture/04-delivery/deployment.md`

- [ ] Start with operator-free Helm release commands from CI: package/lint, render, validate, `helm upgrade --install --atomic --wait`, and verify rollout/health.
- [ ] Pin the release namespace, chart version, image digest, values file, and timeout explicitly.
- [ ] Add a dry-run or protected-environment gate so a pull request cannot mutate the production cluster.
- [ ] Add rollback instructions using `helm history` and `helm rollback`; preserve the Kustomize path until Helm rollback is proven.
- [ ] Reconsider Flux only if the user explicitly requires continuous drift reconciliation. Reconsider Argo CD only if the operational need outweighs the additional controller footprint and recovery complexity.
- [ ] Commit: `ci(k3s): add validated Helm release workflow`.

### Task 6: Validate stateful data and recovery before production cutover

**Files:**
- Read/modify: current PostgreSQL and Redis deployment definitions
- Modify: `docs/architecture/05-operations/data-lifecycle-and-recovery.md` or add the K3s runbook
- Create: backup/restore validation scripts or jobs only if they match the selected ownership model

- [ ] Define whether PostgreSQL and Redis are external services, explicitly managed workloads, or operator-managed workloads.
- [ ] For PostgreSQL, test an encrypted backup and a restore into an isolated namespace/database; record measured RPO/RTO and migration compatibility.
- [ ] For Redis, define persistence and recovery expectations; do not claim HA on a single node.
- [ ] Verify that a node loss does not destroy the only copy of business data; local-path volumes alone are not a backup strategy.
- [ ] If CloudNativePG or Velero is selected, test the controller’s upgrade, restore, and uninstall/recovery behavior before production cutover.
- [ ] Commit: `docs(ops): document K3s data recovery evidence`.

### Task 7: Perform staged cutover, rollback rehearsal, and closeout

**Files:**
- Modify: `docs/architecture/04-delivery/deployment.md`
- Modify: `README.md`
- Modify: the execution checklist/plan

- [ ] Deploy the Helm release to a non-production namespace or dev cluster.
- [ ] Verify frontend-to-backend routing, OAuth callback paths, health probes, metrics, logs, and secret references.
- [ ] Compare Helm and Kustomize service endpoints and workload behavior.
- [ ] Rehearse rollback to the previous chart revision and, separately, to the Kustomize baseline.
- [ ] Measure node CPU, memory, disk, pod restart count, controller health, and application latency under the expected workload.
- [ ] Promote to production only after the resource budget, backup/restore evidence, and rollback rehearsal pass.
- [ ] Update the ownership documentation so future changes do not create a second source of truth.
- [ ] Run the final validation suite and mark the plan complete.

## 5. Verification Commands

The new session should use the commands appropriate to the installed tool versions:

```bash
helm lint deployment/helm/emme
helm template emme deployment/helm/emme -f deployment/helm/emme/values-dev.yaml
helm template emme deployment/helm/emme -f deployment/helm/emme/values-prod.yaml
kubectl kustomize infra/kubernetes/overlays/dev
kubectl kustomize infra/kubernetes/overlays/prod
helm template emme deployment/helm/emme -f deployment/helm/emme/values-prod.yaml > /tmp/emme-prod-helm.yaml
kubectl diff --server-side --dry-run=server -f /tmp/emme-prod-helm.yaml
kubectl get nodes -o wide
kubectl top nodes
kubectl top pods -A
kubectl get crd
kubectl get pods -A
```

Required outcomes:

- Helm output is schema-valid and contract-equivalent to the approved Kustomize baseline, except for documented ownership changes.
- No mutable `latest` image is used in the production path.
- No secret value appears in chart values, rendered manifests, logs, or Git history.
- Every installed controller has a namespace, version, resource budget, health check, upgrade path, and rollback/recovery procedure.
- The single node remains within its CPU, memory, and storage budget under the expected workload.
- Backup restoration and Helm rollback are demonstrated, not merely documented.

## 6. New-Session Handoff Prompt

Paste the following into the next session:

> Continue the plan in `docs/superpowers/plans/2026-09-07-k3s-helm-operator-strategy.md` for the `emme-service` repository. The target is a single-node/low-cost VPS K3s cluster. Do not install operators or modify cluster state yet. First inspect the current Helm chart, Kustomize overlays, K3s packaged components, deployment workflows, and operational policies. Then research current official documentation for Helm, K3s, cert-manager, External Secrets Operator, CloudNativePG, and the selected observability option. Produce and get approval for a design that chooses the minimum operator set. Treat Helm as the application packaging format, keep Kustomize as the rollback/comparison baseline until parity is proven, reuse K3s Traefik/local-path where appropriate, avoid Longhorn/Argo/Redis operators by default, and require immutable images, no committed secrets, resource budgets, backup/restore evidence, and rollback rehearsal. Execute the plan task-by-task with tests and rendered-manifest validation before any production cutover.

## 7. Plan Self-Review

- Scope is intentionally one deployment strategy, with stateful-data and GitOps controllers treated as explicit decision gates rather than bundled work.
- The plan covers the known chart/Kustomize mismatches and preserves a rollback path.
- No operator is presented as mandatory without a workload-specific condition.
- Verification includes rendering, schema/contract checks, resource measurements, backup restoration, and rollback.
- No implementation code or cluster mutation is authorized by this document alone.
