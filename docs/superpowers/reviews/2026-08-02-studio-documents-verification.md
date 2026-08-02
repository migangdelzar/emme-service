# Studio Documents Verification Report

| Field | Value |
|---|---|
| Module | `modules/studio` — `com.emme.studio.documents` |
| Date | 2026-08-02 |
| Branch | `feat/module-plans-normalization` |
| Scope | Tenant-safe persistence and canonical module structure |

## Result

Studio Documents is structurally migrated to the module template. The domain
aggregate owns lifecycle transitions, application services use the grouped API
contracts, and persistence is reached through an application-owned port and
tenant-scoped adapter.

## Boundary evidence

- No production `documents/entity`, `documents/web`, or flat
  `documents/application/DocumentService` implementation remains.
- `DocumentRepository` has no unscoped `findById(UUID)` operation.
- Existing-row persistence saves resolve by `(tenantId, documentId)`.
- `DocumentEntity` contains persistence mapping only; lifecycle transitions are
  implemented by `domain.model.Document`.
- Document and chunk JPA representations remain under
  `adapter/out/persistence/entity`.
- HTTP entry points remain under `adapter/in/web` and depend on focused use-case
  interfaces.
- No embedding/search adapter was invented because Documents currently has no
  direct embedding provider implementation. Future search integration must use
  an application-owned port and technology-owned adapter.

## Schema comparison

The canonical entity mappings were compared with:

- `database/src/main/resources/db/emme-studio/releases/0.1.0/003-documents.sql`
- `database/src/main/resources/db/emme-studio/releases/0.1.0/007-catalog-search.sql`

The document and chunk table names, tenant columns, status values, version,
chunk uniqueness, content fingerprint, and lifecycle timestamps remain aligned.
Search-only columns (`embedding`, `content_tsv`) remain owned by the search
schema and are intentionally not exposed as Document domain state.

## Verification commands

- `./gradlew :modules:tenancy:test --tests com.emme.tenancy.application.process.TenantProvisioningProcessManagerTest --no-daemon --no-configuration-cache` — passed after the retry-safe scheduler fix.
- `./gradlew :modules:studio:spotlessApply :modules:studio:test --tests com.emme.studio.documents.DocumentsPackageConventionTest --tests com.emme.studio.documents.application.service.GetDocumentServiceTest --no-daemon --no-configuration-cache` — passed.
- `./gradlew :modules:studio:spotlessApply :modules:studio:test :modules:studio:check --no-daemon --no-configuration-cache` — passed before the final tenant-safe persistence guardrails; the focused post-change suite also passed.
- `./gradlew :modules:studio:integrationTest --no-daemon --no-configuration-cache` — passed.
- `node scripts/validate-markdown.mjs` — passed.
- `git diff --check` — passed.

Integration shutdown can emit PostgreSQL/Testcontainers connection warnings
after successful completion because the container closes before Spring's JPA
shutdown cleanup. This is test-environment teardown noise, not a failed test.

## Remaining service-wide gates

- Repository-wide Spring Modulith and CI verification.
- Live migration rollback/recovery evidence.
- Final boot-artifact and deployment verification.
