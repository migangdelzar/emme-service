# Backend Test Configuration and Profile Ownership

Test configuration is shared infrastructure. A module must not copy a generic
Spring profile merely because its integration-test source set needs it.

## Canonical ownership

All reusable test profiles and schema helpers belong to the testing fixtures
library:

```text
libraries/testing/src/testFixtures/resources/
├── application-test.yml              # full module tests with H2
├── application-web.yml               # full web tests with H2
├── application-repository.yml        # repository tests with H2
├── application-resttest.yml          # explicit H2 REST profile
├── application-integration-test.yml  # PostgreSQL/Testcontainers profile
└── intTest-schema.sql                 # shared non-JPA integration schema
```

Every module's integration-test source set depends on
`testFixtures(project(":libraries:testing"))`, so Spring can load these
resources from the shared fixture JAR.

## Profile rules

| Profile | Infrastructure | Owner | Activation |
|---|---|---|---|
| `test` | H2 + JPA | `libraries/testing` | `@ActiveProfiles("test")` |
| `web` | H2 + full Spring web context | `libraries/testing` | `@ActiveProfiles("web")` |
| `repository` | H2 + repository context | `libraries/testing` | `@ActiveProfiles("repository")` |
| `resttest` | H2 + explicit REST test context | `libraries/testing` | Explicit opt-in only |
| `integration-test` | PostgreSQL + Testcontainers | `libraries/testing` | `@PostgresIntegrationTest` |

Module-specific configuration is allowed only when a test has a real module
requirement that cannot be represented by the shared baseline. Such an override
must be named `application-<profile>.yml`, contain only the delta, and be
covered by the owning module's test.

## Database ownership

```mermaid
flowchart LR
    PROFILE[Spring test profile] --> FIXTURE[Testing test-fixture JAR]
    FIXTURE --> H2[H2 unit/web/repository database]
    FIXTURE --> POSTGRES[Testcontainers PostgreSQL]
    POSTGRES --> JPA[JPA entity tables]
    POSTGRES --> SQL[intTest-schema.sql infrastructure tables]
    JPA --> MODULITH[Modulith event publication registry]
```

JPA owns entity tables. The shared SQL helper owns only infrastructure tables
that have no JPA entity. Event publication configuration must use the current
Spring Modulith property namespace:

```yaml
spring:
  modulith:
    events:
      jdbc:
        schema-initialization:
          enabled: true
```

Do not use the removed flat `jdbc-schema-initialization` property and do not
reintroduce module-local copies of shared profiles.

## Verification

The profile consolidation is verified by compiling every integration-test
source set and packaging the shared fixture JAR:

```text
./gradlew :libraries:testing:testFixturesJar \
  :modules:assistant:integrationTestClasses \
  :modules:calendar:integrationTestClasses \
  :modules:catalog:integrationTestClasses \
  :modules:identity:integrationTestClasses \
  :modules:notification:integrationTestClasses \
  :modules:payment:integrationTestClasses \
  :modules:studio:integrationTestClasses \
  :modules:tenancy:integrationTestClasses
```

The service-wide CI gate remains authoritative for formatting, compilation,
tests, Checkstyle, and integration-test classpath wiring.
