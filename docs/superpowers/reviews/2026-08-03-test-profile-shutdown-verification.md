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

The shared PostgreSQL container configuration also no longer enables
`withReuse(true)`. Reusable containers allowed the Testcontainers resource
reaper to terminate PostgreSQL before Spring Modulith's JDBC publication
registry completed its shutdown callback.

The test configuration also declares an explicit bean-definition dependency so
the publication registry is destroyed before the PostgreSQL container. This
keeps the JDBC event-publication cleanup connected until the final framework
callback completes.

## Verification

The profile contract is executable in
`PlatformApplicationParityTest.ephemeralTestProfilesKeepSchemasAvailableDuringFrameworkShutdown`.

```text
./gradlew :applications:emme-platform:test \
  --tests com.emme.PlatformApplicationParityTest \
  --no-daemon --no-configuration-cache --console=plain
./gradlew :modules:studio:check \
  --no-daemon --no-configuration-cache --console=plain
./gradlew :modules:shared:integrationTest \
  --tests com.emme.shared.search.HybridSearchIntegrationTest \
  --max-workers=1 --no-daemon --no-configuration-cache --console=plain
./gradlew :modules:identity:integrationTest \
  --max-workers=1 --no-daemon --no-configuration-cache --console=plain
```

All focused checks completed successfully. The Studio check no longer emits the
prior H2 `event_publication` missing-table shutdown warnings, and the focused
Shared and Identity PostgreSQL integration tests complete without the prior
connection-termination diagnostics.

The broader multi-module matrix also passes, but some separately launched Spring
contexts still emit shutdown-only PostgreSQL diagnostics and the Kafka profile
reports an unfinished publication during JVM shutdown. Those remain open
service-wide lifecycle evidence items; they do not fail the integration tests.
