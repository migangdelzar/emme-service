# Task 7 Report — Tenant security, authorization, and idempotency hardening

## Status

Implemented the concrete security/context gaps found in the scoped audit. Existing dirty
identity, tenancy, subscription-adjacent, assistant, database, and task-tracking files were
preserved; only this slice was staged.

## Findings and fixes

1. `TenantContextFilter` selected an authenticated tenant before request selectors, but silently
   ignored a valid conflicting header/query/host selector. It now rejects any resolved caller
   selector that differs from the authenticated tenant with `AccessDeniedException`.
2. `TenantIdentifierResolver` fell back to `emme_core` when a tenant schema registry lookup failed.
   That could route tenant work to the metadata schema. Active-tenant lookup failures now fail
   closed; the no-tenant bootstrap path still resolves to `emme_core`.
3. `TenantActivated` supplied a schema identifier later interpolated into the subscription
   provisioning SQL statement without event-boundary validation. The event now rejects schema
   names outside `[a-z0-9_]+`, preventing malformed or injectable schema identifiers from crossing
   the tenant event boundary.

## Tests added

- `TenantContextFilterTest.rejectsAValidCallerTenantSelectorThatConflictsWithTheAuthenticatedTenant`
- `TenantIdentifierResolverTest.failsClosedWhenTheAuthenticatedTenantSchemaCannotBeResolved`
- `TenantActivatedTest.rejectsASchemaNameThatCouldEscapeTheSubscriptionSchemaStatement`

Each behavioral fix was developed red → green: the new focused test failed against the prior
behavior, then passed after the minimal implementation.

## Verification

- `./gradlew :modules:tenancy:test --tests com.emme.tenancy.adapter.in.web.filter.TenantContextFilterTest`
  — initial regression test failed before the fix; passed after the fix.
- `./gradlew :modules:tenancy:test --tests com.emme.tenancy.api.event.TenantActivatedTest`
  — initial validation test failed before the fix; passed after the fix.
- `./gradlew :modules:tenancy:test --tests com.emme.tenancy.adapter.out.client.database.TenantIdentifierResolverTest`
  — initial fail-closed test failed before the fix; passed after the fix.
- Focused combined tenancy context/event command — **PASS, 6 tests**.
- Focused identity/tenancy context command — **PASS, 50 tests**.
- `git diff --check` — **PASS**.
- Full `:modules:tenancy:test :modules:identity:test` — **LIMITED: 14 failures** from existing
  application-context setup requiring `SemanticCacheDependencyPublisher`; this is outside the
  allowed Task 7 write scope and is unrelated to the changed classes.

## Concerns

- The filter raises `AccessDeniedException`; production security exception translation should map
  this boundary rejection to HTTP 403 according to the application’s filter ordering.
- Existing subscription provisioning remains best-effort and logs duplicate/SQL failures. No
  subscription source change was made because the tenant event now validates its dynamic schema
  identifier and the existing database uniqueness boundary remains the idempotency mechanism.
- Existing dirty files remain unstaged and uncommitted by this slice.
