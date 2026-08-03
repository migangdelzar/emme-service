# Test-profile shutdown verification — 2026-08-03

## Finding

The shared H2 profiles used `spring.jpa.hibernate.ddl-auto: create-drop`.
During JVM shutdown, Hibernate dropped `event_publication` before Spring
Modulith's publication registry callback executed. The tests were green, but
shutdown emitted missing-table and failed-schema-drop diagnostics.

## Change

All ephemeral test-only profiles now use `ddl-auto: create`:

- `applications/emme-platform` test and Kafka integration profiles;
- shared `test`, `repository`, `web`, and `resttest` profiles;
- the shared PostgreSQL integration profile.

The databases remain isolated at startup, while their schemas remain available
until the framework callbacks complete. Test-only databases are disposable, so
explicit Hibernate shutdown drops provide no value here.

## Verification

The profile contract is executable in
`PlatformApplicationParityTest.ephemeralTestProfilesKeepSchemasAvailableDuringFrameworkShutdown`.

```text
./gradlew :applications:emme-platform:test \
  --tests com.emme.PlatformApplicationParityTest \
  --no-daemon --no-configuration-cache --console=plain
./gradlew :modules:studio:check \
  --no-daemon --no-configuration-cache --console=plain
```

Both completed successfully. The Studio check no longer emits the prior H2
`event_publication` missing-table shutdown warnings. PostgreSQL/Testcontainers
connection teardown output remains separately tracked as environment-specific
test-harness evidence.
