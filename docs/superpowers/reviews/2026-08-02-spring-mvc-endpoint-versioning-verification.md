# Spring MVC Endpoint Versioning Verification

| Field | Value |
|---|---|
| Scope | Service MVC boundary |
| Date | 2026-08-02 |
| Status | Verified |

## Decision

The service uses one centralized Spring MVC API-version strategy:

- source: `API-Version` request header;
- default: `1.0` for requests without an explicit header;
- supported version: `1.0`;
- public URI: version-neutral `/api/...` routes;
- controller condition: Identity's `/api/identity` mapping declares
  `version = "1.0"`.

The service is pre-release and does not require backwards-compatible aliases;
the old `/api/v1/...` path is intentionally not maintained.

```mermaid
flowchart LR
    request[HTTP request]
    header[API-Version header]
    strategy[Central ApiVersionConfigurer]
    mapping[Identity mapping version 1.0]
    uri[Version-neutral /api URI]

    request --> header
    header --> strategy
    uri --> mapping
    strategy --> mapping
    mapping --> handler[Controller handler]
```

The resolver is configured in Tenancy's existing global MVC configuration so
the service has one resolver rather than module-specific version policies.

The route migration also updated e2e clients, application configuration,
operational scripts, performance workloads, Kubernetes callback and alert
configuration, and test fixtures. Removing the URI segment exposed a collision
between tenant creation and tenant provisioning; the latter now uses the
explicit `/api/tenant-provisioning` workflow route.

## Verification

```text
./gradlew :modules:tenancy:check :modules:identity:check \
  :applications:emme-platform:test \
  --tests com.emme.ModularityTest \
  --no-daemon --no-configuration-cache
```

Result: `BUILD SUCCESSFUL`.

The output still contains known asynchronous Modulith/Testcontainers shutdown
noise; it did not fail the build and is tracked for the final lifecycle gate.
