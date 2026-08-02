# Service-wide architecture verification — 2026-08-02

## Scope

This report verifies the current `feat/module-plans-normalization` service tree
against the latest module template and the capability-driven outbound adapter
rules. It covers the structural migrations completed on this branch, including
the Assistant AI/WhatsApp slice and technology-owned Notification, Payment, and
Calendar clients. `emme-platform` is the sole deployable application and
composition root.

## Verified architectural outcomes

| Area | Result |
|---|---|
| One application service per public use case | Pass |
| Public commands, queries, results, events, exceptions, and types grouped by kind | Pass for migrated modules |
| Framework-free domain and application-owned outbound ports | Pass for migrated modules |
| Persistence entities/repositories hidden behind outbound adapters | Pass |
| AI clients grouped by technology and injected through a capability-owned transport boundary | Pass |
| WhatsApp parsing, processing, tenant routing, replay claim, and reply delivery separated by adapter boundary | Pass |
| Notification clients grouped by email, SMS, and push channel | Pass |
| Payment clients grouped by provider technology | Pass |
| Calendar OAuth support grouped under the OAuth capability package | Pass |
| External HTTP clients composed at capability roots | Pass for Assistant, Calendar, Notification, and Payment |
| Deterministic provider HTTP contract coverage | Pass for Stripe and Twilio representative contracts |
| Legacy generic provider source packages | No tracked production sources remain |
| Application architecture tests | Pass |
| Spring Modulith named interfaces | Pass for `emme-platform` |

## Canonical application and delivery target

| Surface | Canonical target | Evidence |
|---|---|---|
| Composition root | `applications/emme-platform` | `PlatformApplicationParityTest` |
| Container image | `ghcr.io/migangdelzar/emme-service` | `emmeContainer.imageName` and target validator |
| Compose service | `emme-platform` | `scripts/validate-emme-platform-target.mjs` |
| Legacy Kubernetes workload | `emme-platform` | Kustomize local/production render |
| Kubernetes provider workload | `backend` / `app=emme-backend` | `KubernetesWorkloadTest` |
| Application project count | One (`emme-platform`) | `settings.gradle.kts` and Gradle project graph |

## Verification commands

All commands below completed successfully with `--no-configuration-cache` and a
single-use Gradle daemon:

```text
./gradlew check --quiet --no-daemon --no-configuration-cache
./gradlew :modules:shared:integrationTest :modules:identity:integrationTest \
  :modules:tenancy:integrationTest :modules:catalog:integrationTest \
  :modules:studio:integrationTest :modules:assistant:integrationTest \
  :modules:notification:integrationTest :modules:payment:integrationTest \
  --quiet --no-daemon --no-configuration-cache
./gradlew ci -x test -x integrationTest -x e2eTest \
  --quiet --no-daemon --no-configuration-cache
./gradlew :applications:emme-platform:bootJar \
  --quiet --no-daemon --no-configuration-cache
node scripts/validate-markdown.mjs
git diff --check
```

Focused red/green tests also passed for:

- canonical application architecture rules and named-interface closure;
- Assistant AI client package ownership and typed configuration;
- WhatsApp webhook mapping, signature verification, tenant routing, replay
  claim, and duplicate suppression;
- Notification and Payment technology-owned client packages;
- Calendar OAuth package ownership and Google transport injection;
- Stripe request/authentication/error mapping through a local HTTP contract;
- Twilio request/authentication/error mapping through a local HTTP contract.

## Operational notes

Some create-drop tests emit shutdown-only warnings when the in-memory database
has already been closed before Spring Modulith's event-publication cleanup
queries run. The test tasks still complete successfully. PostgreSQL and
Testcontainers teardown may also emit connection/prune warnings after a green
test run; these do not indicate a failed assertion or an application startup
failure.

Live third-party provider contract execution and a dedicated PostgreSQL replay
demonstration remain operational evidence items in the module plans. They are
separate from the completed package migration and must be run with provider
credentials/containers available.

Kafka transport replacement is now implemented as the Spring Modulith Kafka
externalization slice. The dedicated Gradle build-logic CDD follow-up remains
ordered for its later phase.

## Commits included in this verification slice

- `ef317f0` — canonical application architecture rules and package placement
- `3a320a9` — Assistant AI and WhatsApp adapter boundaries
- `42bd5db` — technology-owned delivery client packages
- `696e5f1` — service-wide migration verification report
- `c0ee541` — Notification and Payment transport injection
- `d5f3b8e` — Calendar Google transport injection
- `389ec87` — deterministic Stripe and Twilio provider contracts
- `b030052` — canonical delivery formatting
- `c3ece72`, `3a10dee` — application-level Google package-rule alignment
