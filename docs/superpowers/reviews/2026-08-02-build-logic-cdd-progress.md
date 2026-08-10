# Build-Logic CDD Progress

| Field | Detail |
|---|---|
| Build | `build-logic` included build |
| Branch | `feat/module-plans-normalization` |
| Date | 2026-08-02 |
| Status | Implemented and verified for the current unreleased service |

## Completed slice

- Added `ArchitectureInventoryTest` to enforce the convention-script inventory
  and capability ownership locations.
- Added `PluginIdContractTest` to protect all public convention IDs and binary
  implementation registrations.
- Renamed publishing task classes to `*Task` names.
- Renamed unreleased publishing task IDs from `publishInfo`/`publishVerify` to
  `publishBuildInfo`/`publishVerifyVersion`.
- Did not add aliases because this build is unreleased and no external consumer
  requires backwards compatibility.
- Removed the container extension from `EmmeBuildExtension`; the root extension
  now contains repository-wide metadata only.
- Renamed container task implementation types to `BuildContainerImageTask`,
  `PushContainerImageTask`, and `VerifyContainerImageTask` while preserving the
  registered task names.
- Separated `ContainerPushResult` and `RegistryPushResult` so the registry
  capability does not depend on container implementation types.
- Added truthful lazy provider selection for Docker/Podman, Compose/Kubernetes,
  and Trivy/Grype; unsupported selector values now fail with supported-values
  diagnostics instead of silently selecting a different provider.
- Typed deployment and security selector properties with capability-owned enum
  models and added a default container context directory.
- Added TestKit coverage for deployment and security task registration,
  configuration-time laziness, and execution-time invalid-selector failures.
- Added publishing TestKit coverage for task registration, metadata generation,
  invalid semantic versions, and operation outside a Git checkout.
- Made Git branch/commit ValueSources tolerate non-Git temporary projects with
  deterministic `unknown` values and no fatal stderr output.
- Added quality and API compatibility TestKit coverage; quality formatting now
  works as a composable capability without requiring the Java plugin, and Sonar
  report paths remain provider-backed.
- Added convention capability TestKit coverage for Java library, testing,
  test-fixtures, persistence, Kafka messaging, Modulith, and Spring Web
  composition with explicit project-platform fixtures.
- Added root plugin TestKit coverage for repository lifecycle task registration.
- Verified the complete functional suite with Gradle configuration cache both
  on a cold run and on a second run that reused the cache entry.
- Separated module-type conventions from capability conventions: persistence,
  messaging, Modulith, and Spring Web no longer apply `emme.spring-module`
  implicitly. Modules opt into their type and capabilities explicitly.
- Added an architecture regression test that rejects capability scripts applying
  module-type plugins and a TestKit configuration-cache functional test.
- Verified that the deployable application no longer emits the previous
  `java-library`/Spring Boot composition warning caused by hidden module-type
  composition.

## Verification

```text
./gradlew :build-logic:check --no-daemon --no-configuration-cache --console=plain
```

Result: `BUILD SUCCESSFUL`. Spotless, Detekt, unit tests, functional TestKit
tests, configuration-cache coverage, and Gradle plugin validation completed
successfully. The JVM still prints the restricted-native-access warning for
Zstandard/Gradle native libraries; that warning is unrelated to build-logic
correctness.

## Final boundary

The build-logic CDD migration is closed for this unreleased service branch.
Future capabilities must follow the same rules: module-type plugins establish
what a project is, capability conventions add optional behavior, and complex
capabilities own their plugins, extensions, tasks, providers, results, and
tests. New external-tool integrations require a capability-owned provider and
TestKit coverage before adoption.
