# Repository Cleanup Audit

| Field | Value |
|---|---|
| Date | 2026-08-03 |
| Repository | `emme-service` |
| Scope | Empty directories, generated artifacts, duplicate metadata, and provably unreachable source |
| Status | Verified; no speculative production deletion performed |

## Evidence

| Check | Result |
|---|---|
| Tracked build output | None found in the Git index |
| Tracked generated reports/classes | None found in the Git index |
| Empty directories | Removed locally where safe; Git does not version directories |
| Duplicate Java package metadata | One Assistant declaration corrected |
| Java compilation and module checks | Passed with `:modules:assistant:check` |
| Deployable boot JAR | Passed with `:applications:emme-platform:bootJar` |
| Service CI gate | Passed with `./gradlew ci` |

## Corrected metadata

`modules/assistant/.../adapter/in/web/controller/package-info.java` was
declaring the AI controller package even though it physically belongs to the
conversation controller package. That created duplicate `package-info.java`
metadata for `com.emme.assistant.ai.adapter.in.web.controller` during boot-JAR
packaging. The declaration now matches its owning directory and documents the
conversation HTTP controllers.

## Unused-code policy

The repository has no reliable compiler task that proves a Java class is
unreachable: Spring component scanning, reflection, Modulith event listeners,
configuration properties, and public module contracts can make name-based
searches unsafe. Therefore this audit removes only evidence-backed artifacts
and metadata defects.

Before deleting a production type, require all of the following:

1. A repository-wide reference search finds no consumer, configuration, event,
   test, or public contract reference.
2. Modulith, architecture, compilation, and relevant integration tests remain
   green after removal.
3. The type is not a Spring bean, configuration property, serialization
   contract, migration owner, or provider selected by configuration.
4. The deletion is recorded in the owning module migration plan.

Gradle dependency analysis remains the source of truth for unused dependency
declarations. It does not replace this source-level review and must not be used
as justification for deleting valid ports or adapters.

## Follow-up

The remaining cleanup opportunity is warning reduction in the frontend lint
baseline and any future service type proven unreachable by the rules above.
Neither should be addressed by broad pattern-based deletion.
