# Task 3 implementation report

## Status

COMPLETE_WITH_LIMITATION. Task 3 reviewer findings have been addressed within the assistant/catalog/database scope.

## Changes

- Added shared tenant image read/write contracts in `ai-contracts` so assistant does not depend on catalog.
- Extended catalog image storage with tenant-scoped reads and path traversal protection.
- Added `CatalogDesignImageReader`, using the authenticated `AiExecutionContext` tenant.
- Added multipart `/api/ai/quotes` submission boundary with content-type and 10 MiB validation.
- Added quote request/response records and metadata repository port.
- Reader now requires an explicit execution context and rejects active-scope tenant mismatches.
- Reader is consumed by the existing Spring AI vision extraction path.
- Added compensating storage deletion when metadata persistence or quote processing fails.
- Compensating cleanup also covers late multipart read failures after storage succeeds.
- Metadata deletion is part of compensating cleanup; metadata and object deletion are attempted independently so retries can proceed even if one cleanup operation fails.
- The production submission path initializes the workflow row through the quote use-case boundary before inserting image metadata, preserving the `ai_design_image.workflow_id` foreign key.
- Removed the silent ImageStorage read default; local storage returns SHA-256 metadata.
- Added durable `ai_design_image` metadata migration with RLS and changelog registration.

## Verification

- `mise exec java@25.0.2 -- ./gradlew :modules:assistant:test :modules:assistant:integrationTest :modules:catalog:test :database:test :modules:assistant:spotlessCheck --quiet` — passed.
- `mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests '*CatalogDesignImageReaderTest' --tests '*SpringAiNailDesignExtractorTest' --tests '*SpringAiQuoteExtractionConfigurationTest' :modules:catalog:test --quiet` — passed.

## Review finding resolution

- Added `JdbcDesignImageMetadataRepository`; metadata is persisted with tenant/workflow uniqueness and PostgreSQL RLS.
- Wired metadata persistence into the submission boundary after tenant-scoped storage.
- Added `DesignQuoteControllerTest`, `CatalogDesignImageReaderTest`, and `AiDesignImageMigrationContractTest`; reader tests include missing and mismatched contexts.
- Synchronous quote invocation remains deliberately bounded to this task; asynchronous orchestration is deferred to the planned job phase.

## Additional verification

- `mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests '*DesignQuoteControllerTest' --tests '*CatalogDesignImageReaderTest' :database:test --tests '*AiDesignImageMigrationContractTest' :modules:assistant:integrationTest :modules:catalog:test :modules:assistant:spotlessApply :modules:assistant:spotlessCheck --quiet` — passed.

## Remaining limitations

- Database tests are migration contract assertions; PostgreSQL/Testcontainers infrastructure is not enabled in this module, so live RLS enforcement is not exercised.
- Controller tests remain unit-level rather than full authenticated MockMvc coverage because the existing security test harness does not expose tenant context setup for this endpoint.
- Local storage tests cover symlink escape resistance for reads and deletes.
