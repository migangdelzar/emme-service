# Build-Logic CDD Progress

| Field | Detail |
|---|---|
| Build | `build-logic` included build |
| Branch | `feat/module-plans-normalization` |
| Date | 2026-08-02 |
| Status | Guardrails, capability ownership, lazy provider selection, and container task naming slices complete; broader CDD migration open |

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

## Verification

```text
./gradlew :build-logic:check --no-daemon --no-configuration-cache --console=plain
```

Result: `BUILD SUCCESSFUL`. Spotless, Detekt, unit tests, functional TestKit
tests, and Gradle plugin validation completed successfully. The build still
prints the existing dependency-analysis warnings from `emme-platform` and the
JVM restricted-native-access warning from Gradle's native platform library.

## Next CDD slices

1. Complete the remaining `core`/`model`/root shared-service audit.
2. Add TestKit coverage for every foundation, testing, persistence, messaging,
   Modulith, delivery, security, and quality convention family.
3. Verify lazy provider selection, task input/output declarations, and
   configuration-cache behavior.
4. Run the complete build-logic and service-wide final gate.
