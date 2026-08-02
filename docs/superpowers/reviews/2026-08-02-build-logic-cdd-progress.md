# Build-Logic CDD Progress

| Field | Detail |
|---|---|
| Build | `build-logic` included build |
| Branch | `feat/module-plans-normalization` |
| Date | 2026-08-02 |
| Status | Guardrails and publishing naming slice complete; broader CDD migration open |

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

## Verification

```text
./gradlew :build-logic:check --no-daemon --no-configuration-cache --console=plain
```

Result: `BUILD SUCCESSFUL`. Spotless, Detekt, unit tests, functional TestKit
tests, and Gradle plugin validation completed successfully. The build still
prints the existing dependency-analysis warnings from `emme-platform` and the
JVM restricted-native-access warning from Gradle's native platform library.

## Next CDD slices

1. Enforce core/model/root ownership and remove capability leakage.
2. Add TestKit coverage for every foundation, testing, persistence, messaging,
   Modulith, delivery, security, and quality convention family.
3. Verify lazy provider selection, task input/output declarations, and
   configuration-cache behavior.
4. Run the complete build-logic and service-wide final gate.
