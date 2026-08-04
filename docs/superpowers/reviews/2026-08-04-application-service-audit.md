# Application-Service Transaction and Dependency Audit

| Field | Value |
|---|---|
| Repository | `emme-service` |
| Branch | `feat/enterprise-module-template-conformance` |
| Date | 2026-08-04 |
| Scope | Production services under `modules/**/application/service` |
| Rule source | [Enterprise module conformance plan](../plans/2026-08-03-enterprise-module-template-conformance.md) |

## Result

The repository contains 123 concrete application services. Every service
implements exactly one matching use-case interface, and every service now has an
explicit transaction policy unless it is an external-only integration service
listed below.

| Policy | Count | Interpretation |
|---|---:|---|
| `@Transactional` | 69 | State-changing workflow boundary |
| `@Transactional(readOnly = true)` | 47 | Read-only workflow boundary |
| Explicitly non-transactional | 7 | External-only or orchestration work with no local persistence boundary |

The two missing read boundaries found by the audit were added to
`GetCurrentUserService` and `GetGoogleOAuthStatusService`. External calls are
not wrapped in database transactions merely to satisfy a numeric rule.

## Explicit non-transactional services

These services intentionally do not open a database transaction:

| Service | Reason |
|---|---|
| `AuthenticateUserService` | Coordinates identity-provider authentication and realm selection; the provider call is external. |
| `CaptionImageService` | Delegates to an AI model provider. |
| `ChatService` | Delegates to an AI model provider. |
| `DetectIntentService` | Delegates to an AI model provider. |
| `EmbedTextService` | Delegates to an AI model provider. |
| `RagQueryService` | Coordinates model-provider calls and a read use case; no local aggregate mutation. |
| `StartGoogleOAuthService` | Builds an external consent URL; it does not persist state. |

The initial scan found nine non-transactional services; the two missing read
boundaries were then fixed. The architecture guardrail uses service names as
the documented exemption policy and rejects accidental transaction annotations
on these external-only services.

## Constructor dependency review

Constructor dependency count is a design signal, not an arbitrary compile-time
limit. One to three dependencies is the normal range. Four or five dependencies
is a review candidate when the use case coordinates multiple aggregates or
providers. No service currently exceeds five constructor dependencies.

| Service group | Dependencies | Decision |
|---|---:|---|
| `Studio` appointment create/read/update lifecycle services | 5 | Retain: they coordinate appointment collision, customer, service, and artist ports for one appointment use case. Review if the appointment aggregate or read model is split later. |
| `Catalog.MatchCatalogItemsService` | 5 | Retain: image/text AI, search, and catalog persistence are one search use case. Keep the ports application-owned. |
| `Notification.DeliverNotificationService` | 5 | Retain: channel strategy selection and delivery publication are one delivery use case. Consider a channel router only when channels become independently configurable. |
| `Assistant.ProcessWhatsAppMessageService` | 5 | Retain: it coordinates the WhatsApp workflow through use-case interfaces and a participant port. It is a process boundary, not a reusable helper. |
| `Catalog.AddCatalogItemImageService` | 4 | Retain: persistence, storage, and captioning are required by one image-ingestion use case. |
| `Identity.GetCurrentUserService` | 4 | Retain: it composes membership, permissions, tenant, and business-profile public read contracts into one current-user result. |

These services are not split into artificial helper classes. If a future change
increases their responsibility, extract a named application port, read facade,
or process coordinator with an independent reason to change. Do not merge use
cases or introduce a generic `Manager`/`Helper` class.

## Executable guardrails

- `ApplicationServiceArchitectureTest` verifies one matching use case per
  service.
- The same test verifies that every application service declares a transaction
  policy or is listed in the explicit external-only exemption set.
- `DddHexagonalArchitectureTest` verifies application code does not import
  outbound adapters or persistence frameworks.
- `CrossModuleDependencyArchitectureTest` verifies cross-module dependencies
  use public API contracts only.

## Verification

```text
./gradlew :applications:emme-platform:test --tests com.emme.ApplicationServiceArchitectureTest --no-daemon
./gradlew :modules:shared:test :modules:calendar:test :applications:emme-platform:test spotlessCheck --no-daemon
```

Both commands pass on this branch. The repository pre-push gate also runs the
full module test suite, JaCoCo report/verification, and coverage check.

## Follow-up

The next service-level review should inspect transaction propagation and retry
semantics at external-provider boundaries, especially payment, notification,
calendar, and tenant provisioning. Constructor count alone is not evidence that
those workflows need refactoring.
