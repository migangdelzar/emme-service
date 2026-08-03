# Baseline Gaps

> **Naming contract:** Follow the [canonical architecture naming catalog](../00-project/naming-conventions.md) for package names, filenames, Java/Kotlin types, methods, and tests. Local examples on this page must not introduce a conflicting convention.

This is a dated audit, not an architecture rule. Update it when evidence changes.

| Gap | Risk | Owner | Exit evidence |
|---|---|---|---|
| Cross-repository real-stack E2E is not a default CI gate | Contract drift can reach release | Service + web owners | Coordinated E2E workflow with exact refs |
| Container publishing permissions are not fully automated | Manual release or wrong registry risk | Delivery owner | Least-privilege publish job and digest evidence |
| Existing monorepo public history contains historical recordings | Credential/data exposure in history | Repository owner | Rotate tokens and complete approved history cleanup |
| SLO dashboards and restore exercises need production evidence | Recovery claims are unverified | Operations owner | Measured SLO and restore reports |

The split repositories are intentionally usable before these gaps are closed, but
the gaps MUST remain visible and must not be described as completed controls.
