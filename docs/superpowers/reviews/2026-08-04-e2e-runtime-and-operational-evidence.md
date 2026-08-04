# E2E runtime and operational evidence — 2026-08-04

## Scope

This checkpoint records the current local evidence for the disposable
full-stack runtime on `feat/enterprise-module-template-conformance` and the
real recording suite on `emme-web` branch `feat/api-version-contract`.

It complements the repository-wide architecture verification report. It does
not claim deployment-environment evidence that requires production credentials,
an external broker, a real database outage, or a native-image-capable runner.

## Verified locally

| Area | Result | Evidence |
|---|---|---|
| Keycloak OIDC provisioning | Pass | Realm provisioning creates the required `profile`, `email`, and `roles` scopes; token and user-info requests succeed |
| Tenant provisioning replay | Pass | Two consecutive provisioner executions succeeded and returned the same tenant identifier `d773013c-52a7-44d4-8068-2b3004330c35` |
| Tenant schema routing | Pass | Disposable tenant JDBC URL uses `e2e_studio,emme_core,public` search-path ordering; E2E API login and authenticated module requests succeed |
| JVM image health | Pass | `emme-service:local-health` starts with the shell-free Java actuator probe and Docker reports `healthy` |
| Native/JVM Compose contracts | Pass | `docker-compose config --quiet` passes for both runtime overlays; native explicitly disables the JVM healthcheck |
| Service focused verification | Pass | Shared, Identity, Tenancy, Assistant, Notification, Payment, and `emme-platform` tests/coverage pass |
| Service deployment contract | Pass | Compose contract tests pass, including migration context, tenant schema, Keycloak configuration, and runtime probe rules |
| Real full-stack recordings | Pass | Canonical tenant-owner recording suite passes 5/5 with one worker and zero retries |
| Recording artifacts | Pass | Playwright videos, traces, screenshots, and report are generated under `emme-web/e2e/src/test-results/real-recordings` |
| Web quality gates | Pass | E2E typecheck, recording contract tests, frontend coverage, i18n validation, and workspace lint pass; lint has existing non-blocking warnings only |
| Legacy runtime audit | Pass | No RabbitMQ/AMQP runtime integration, active `studio-api` application target, or production direct JDBC connection acquisition remains |
| Endpoint versioning audit | Pass | Internal controller mappings use Spring header version `1.0`; the unversioned WhatsApp callback remains an external-provider transport contract |

## Commands and outcomes

### Service

```text
./gradlew :applications:emme-platform:spotlessApply \
  :applications:emme-platform:coverageCheck \
  :modules:tenancy:test :modules:identity:test :modules:assistant:test \
  :modules:notification:test :modules:payment:test :modules:shared:test \
  :applications:emme-platform:test \
  --no-daemon --no-configuration-cache --console=plain
```

Result: `BUILD SUCCESSFUL`.

```text
node deployment/compose/compose.e2e.contract.test.mjs
```

Result: all Compose and provisioning contract tests pass.

```text
docker-compose -f deployment/compose/compose.yaml \
  -f deployment/compose/compose.runtime-jvm.yaml config --quiet
docker-compose -f deployment/compose/compose.yaml \
  -f deployment/compose/compose.runtime-native.yaml config --quiet
```

Result: both runtime configurations render successfully.

### Web

```text
bun run --filter @emme/e2e typecheck
bun run --filter @emme/e2e test:real:recordings
```

The canonical recording command is deliberately narrow and deterministic:

```text
E2E_MODE=real RECORD_DEMO=true \
  bunx playwright test specs/demo/real-demo-recordings.spec.ts \
  --project=real --workers=1 --retries=0
```

Result: five tenant-owner recordings pass:

1. owner dashboard;
2. service lifecycle;
3. customer appointment;
4. business settings;
5. finances and navigation.

CI uploads the generated videos, traces, screenshots, and reports after
diagnostics are collected. The local artifact directory is ignored by Git.

## Explicit remaining release gates

These are not represented as complete because they require infrastructure or
credentials unavailable to the local verification run:

- live PostgreSQL pool eviction and recovery during an actual database outage;
- provisioning rollback and backup/restore rehearsal;
- credentialed Keycloak migration/recovery;
- credentialed Twilio, MessageBird, Vonage, payment, and AI provider calls;
- broker outage and deployed Kafka consumer-recovery drills;
- GraalVM native-image build and JVM-versus-native memory/latency measurements;
- shutdown-time publication-registry diagnostics in every separately launched
  Spring context;
- changing CI image/runtime references from feature branches to the default
  branch after the split-repository changes merge.

These gates remain tracked as operational acceptance work. They do not block
the local architecture or real full-stack recording evidence described above.
