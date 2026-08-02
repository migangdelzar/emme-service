# Emme Platform Cutover Verification — 2026-08-02

## Outcome

`emme-platform` is now the sole application project and deployable service.
The obsolete `applications/studio-api` project was removed after verifying that
the platform application already contained the newer runtime, Kafka, Jackson,
configuration, and application-test implementation.

The Studio business module remains a real Modulith module. Its named interface
vocabulary such as `studio-api` refers to a module contract, not to a deleted
Spring Boot application.

## Migration decisions

| Area | Decision |
|---|---|
| Application composition root | Keep `applications/emme-platform` only |
| Build graph | Remove `:applications:studio-api` from `settings.gradle.kts` |
| CI | Run platform tests and boot JAR only |
| Container image | `ghcr.io/migangdelzar/emme-service` |
| Compose service | `emme-platform` |
| Legacy Kubernetes workload | `emme-platform`, port `8081` |
| Canonical Kubernetes workload | `backend`, selector `app=emme-backend` |
| Helm image repository | `ghcr.io/migangdelzar/emme-service` |
| Kafka | Preserved in the platform application; not required by local/test profiles |
| Legacy-only demo seed/configuration | Not copied; it targeted the obsolete `demo_tenant` schema and old runtime profiles |

## Code migration inventory

The platform application already owned the current versions of the relevant
production behavior before deletion:

- `EmmeApplication` and the current Spring Boot composition root;
- Kafka/Spring Modulith externalization configuration and integration test;
- shared Jackson configuration with module registration;
- current application profiles and typed configuration consumed by the active modules;
- current architecture, Modulith, schema ownership, and platform parity tests;
- current E2E client/test source set.

The deleted project contained older-only artifacts that were intentionally not
copied because they would reintroduce deprecated behavior:

- `DataSeeder` and `db/demo/data.sql`, which target the legacy `demo_tenant`
  schema and bypass the current migration/tenant lifecycle;
- `application-docker.yml`, `application-local-dev.yml`, and
  `application-postgres.yml`, which use obsolete ports, Hibernate schema update,
  and disabled migration behavior;
- the older role/login E2E harness and duplicate application smoke tests. The
  platform E2E harness and current application tests are the maintained source.

## Verification evidence

All completed successfully:

```text
node --test scripts/validate-emme-platform-target.test.mjs
node scripts/validate-emme-platform-target.mjs
node scripts/validate-markdown.mjs
./gradlew projects --no-daemon --no-configuration-cache
./gradlew :build-logic:check :applications:emme-platform:test \
  :applications:emme-platform:bootJar --no-daemon --no-configuration-cache
kustomize build infra/kubernetes/overlays/dev
kustomize build infra/kubernetes/overlays/prod
kustomize build deployment/kubernetes/overlays/local
kustomize build deployment/kubernetes/overlays/production
git diff --check
```

The Gradle project graph no longer contains `:applications:studio-api`.
The target validator confirms that settings, CI, `mise`, Compose, Helm values,
Kubernetes manifests, deployment scripts, and the Kubernetes provider contain
no active deleted-application target.

Kustomize rendered all four checked overlays successfully. The local machine
does not provide a Helm executable or Docker Compose plugin, so Helm template
rendering and Compose normalization remain CI/tooling checks rather than local
evidence; the Helm values and Compose files are covered by the deterministic
target validator.

## Remaining reference classification

Remaining textual references to `studio-api` are limited to one of these
intentional categories:

1. validator fixtures and forbidden-token rules, which prove deleted targets are
   rejected;
2. historical migration plans that preserve the original execution record;
3. the Studio Modulith named interface (`studio :: studio-api`), which is a
   business-module API name and not an application project.

No remaining Gradle application include, CI boot/test target, deployment
manifest, image, deployment script, or active build-logic workload points to a
`studio-api` application.
