# Studio Subscriptions Verification Report

| Field | Value |
|---|---|
| Module | `modules/studio` — `com.emme.studio.subscriptions` |
| Date | 2026-08-02 |
| Branch | `feat/module-plans-normalization` |
| Scope | Tenant-safe persistence boundary |

## Result

Studio Subscriptions remains aligned with the canonical module template. Its
public contracts are grouped, application services are one-use-case-per-service,
and persistence is hidden behind an application-owned repository port.

## Boundary evidence

- Subscription reads are tenant-scoped through `findByTenantId`.
- Existing-row saves are tenant-scoped through
  `findByTenantIdAndId(tenantId, subscriptionId)`.
- No application-facing repository method accepts an unscoped subscription ID.
- Payment providers remain outside the Subscriptions capability.
- `SubscriptionEntity` is a persistence representation; plan entitlement rules
  remain in the domain model/policy.
- The stable `PlanType` vocabulary remains under `api/type` and is used by the
  public command/result contracts.

## Schema comparison

The entity and domain values were compared with
`database/src/main/resources/db/emme-studio/releases/0.1.0/002-initial-studio-schema.sql`:

- `subscription.tenant_id` remains unique and is the ownership boundary.
- `plan` preserves `STARTER`, `PRO`, and `ENTERPRISE`.
- `status` preserves `TRIAL`, `ACTIVE`, `PAST_DUE`, `SUSPENDED`, and `CANCELLED`.
- `period_ends_at` remains required.
- The existing HTTP paths and response fields remain unchanged.

## Verification commands

- `./gradlew :modules:studio:spotlessApply :modules:studio:test --tests com.emme.studio.subscriptions.SubscriptionPackageConventionTest --no-daemon --no-configuration-cache` — passed.
- `./gradlew :modules:studio:spotlessApply :modules:studio:test :modules:studio:check --no-daemon --no-configuration-cache` — passed after the persistence guardrail change.
- `./gradlew :modules:studio:integrationTest --no-daemon --no-configuration-cache` — passed.
- `node scripts/validate-markdown.mjs` — passed.
- `git diff --check` — passed.

Integration shutdown may emit existing PostgreSQL/Testcontainers cleanup
warnings after successful completion; no test failed.

## Remaining service-wide gates

- Payment-boundary integration and provider contract evidence.
- Repository-wide Spring Modulith and CI verification.
- Live migration rollback/recovery and boot-artifact verification.
