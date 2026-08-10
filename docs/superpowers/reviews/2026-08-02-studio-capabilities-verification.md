# Studio Nested Capabilities Verification

| Capability | Source migration | Focused/module/integration gate | Remaining |
|---|---|---|---|
| Documents | Canonical DDD + Hexagonal layout complete | Passed Studio tests, integration tests, and `emme-platform` Modulith verification | Shared lifecycle, schema rollback, service-wide CI/boot evidence |
| Subscriptions | Canonical DDD + Hexagonal layout complete | Passed Studio tests, integration tests, and `emme-platform` Modulith verification | Shared lifecycle, payment boundary, schema/security/recovery evidence |

## Verification command

```text
./gradlew :modules:studio:test :modules:studio:integrationTest \
  :applications:emme-platform:test --tests com.emme.ModularityTest \
  --no-daemon --no-configuration-cache
```

Result: `BUILD SUCCESSFUL`; the focused source and integration assertions
completed without test failures.

## Structural result

Documents and Subscriptions each expose grouped public contracts, focused
one-use-case application services, framework-free domain models, tenant-scoped
persistence ports, adapter-owned entities, and Spring Modulith named interfaces.
The removed `studio-api` application is not required by either capability.

## Open evidence

The run still emitted shutdown-time PostgreSQL/Testcontainers and Spring
Modulith JDBC publication-registry warnings, including H2 `event_publication`
absence in some contexts. This remains a shared lifecycle defect, not a reason
to weaken module boundaries or retain compatibility code.
