# FCR-001: Java 25 Baseline

## Change requested

Make Java 25 the only supported Java version across Gradle, build logic, local
Mise, CI, containers, Kubernetes, tests, and JVM runtime validation.

## Affected areas

```text
build-logic
gradle/libs.versions.toml
mise.toml and scripts/mise
.github workflows/actions
deployment and infra images
Testcontainers
documentation and release checks
```

## Acceptance

- Local and CI runtime checks report Java 25.
- Compile/test/JavaExec preview flags are consistent.
- JVM and native-image tracks are explicitly distinguished.
- Legacy active Java 17/21 configuration is removed or marked historical.

## Current implementation evidence

- `scripts/verify-java25-runtime.mjs` fails fast for a runtime below Java 25.
- `mise run toolchain:jvm` validates the local JVM and Gradle runtime lane.
- `mise run toolchain:native` exposes the explicit native-image lane without
  changing the default JVM build.
- Backend CI runs the same runtime validator after installing Temurin Java 25.
