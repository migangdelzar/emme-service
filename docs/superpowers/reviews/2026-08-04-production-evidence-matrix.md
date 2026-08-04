# Production evidence matrix — 2026-08-04

This matrix distinguishes deterministic repository evidence from acceptance
tests that require credentials, a live outage, or a deployed environment.

| Capability | Deterministic evidence already present | Remaining acceptance evidence |
|---|---|---|
| Tenancy | Pool eviction/closure, default-pool recovery, routing, provisioning replay/idempotency, and tenant schema adapter tests | Real database outage, rollback/restore, audit correlation, and live pool recovery |
| Identity | JWT trust, rate limiting, membership authorization, Keycloak provisioning retry, consumer conditions, and security web tests | Credentialed Keycloak migration, rollback, recovery, and privilege-escalation drill |
| Assistant | WhatsApp signature/replay claim tests, tenant resolver tests, AI provider contracts, and PostgreSQL claim integration test | Credentialed provider execution and PostgreSQL replay under restart/failure |
| Notification | Provider contract tests, unsupported-channel guard, tenant-scoped delivery, and delivery-boundary tests | Credentialed provider calls, transient retry/backoff, durable replay, and provider outage recovery |
| Payment | Provider contracts, webhook signature validation, idempotency ports, tenant-scoped operations, and callback tests | Credentialed provider calls, PostgreSQL webhook replay, settlement recovery, and outage handling |
| Shared | Throwable JDBC executor tests, tenant-owned entity tests, i18n/problem-detail tests, and shutdown-order documentation | Clean shutdown in every separately launched context and repository-wide rollback evidence |
| Kafka + Modulith | Event contract tests, Kafka Testcontainer publication, tenant partition keys, idempotent consumers, and restart republication configuration | Deployed broker outage, consumer restart, lag/replay, quarantine, and schema rollback drill |
| Build-logic CDD | Unit/TestKit tests, lazy provider/task contracts, stable plugin IDs, and configuration-cache verification | Future capabilities must preserve the same ownership and TestKit contracts |
| Native delivery | JVM/native Compose and Kubernetes manifest contracts plus native convention TestKit coverage | Actual GraalVM image build, startup, memory, latency, and production smoke test |
| CVE scanning | Gitleaks and dependency verification pass; workflow supports fail-closed NVD execution | Configure `NVD_API_KEY` and run `Security Scan` with `require_nvd=true` |

## Required evidence commands

Repository-local evidence is covered by the green service CI run and these
focused suites:

```text
./gradlew ci --no-daemon --no-configuration-cache --console=plain
./gradlew integrationTest --no-daemon --no-configuration-cache --stacktrace
./gradlew :applications:emme-platform:coverageCheck --no-daemon --no-configuration-cache
./gradlew :build-logic:check --no-daemon --no-configuration-cache
node scripts/validate-markdown.mjs
```

The live gates must be run through a protected environment with redacted
credentials, isolated tenant data, and archived logs/traces. They must not be
faked with a mock provider and must not be silently marked as complete.
