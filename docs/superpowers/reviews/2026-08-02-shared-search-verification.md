# Shared Infrastructure Verification

| Field | Detail |
|---|---|
| Module | `modules/shared` |
| Date | 2026-08-02 |
| Status | Search and tenant-predicate evidence verified; lifecycle gate open |

## Verified boundaries

- `HybridSearch` owns the shared search capability.
- Search and embedding-maintenance queries require an explicit tenant ID.
- Search target predicates are applied by the shared capability rather than by
  individual business modules.
- The integration fixture covers active-row filtering, tenant isolation,
  bounded missing-embedding maintenance, and deterministic result ordering.
- Shared persistence, time, JDBC connection execution, web advice, and identity
  primitives remain capability-owned and do not become business-module APIs.

## Verification

```text
./gradlew :modules:shared:test \
  :modules:shared:integrationTest \
  :applications:emme-platform:test \
  --tests com.emme.ModularityTest \
  --tests com.emme.LayerConventionTest \
  --no-daemon --no-configuration-cache
```

Result: `BUILD SUCCESSFUL`; Shared unit/integration tests and application
Modulith/layer tests completed without test failures.

## Open lifecycle evidence

The integration run still emitted shutdown-time PostgreSQL/Hikari warnings
while the reusable Testcontainers database was terminating. The warnings occur
after the test assertions and do not fail Gradle, but they prevent a clean
service-wide readiness claim. The remaining work is to make Testcontainers,
the tenant pool provider, Hibernate, and Spring Modulith's publication registry
close in a deterministic order, then repeat this gate without shutdown SQL
errors.
