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
