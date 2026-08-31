# Task 3 implementation report

## Status

DONE_WITH_CONCERNS. The authenticated image submission and tenant-safe image reader slice is implemented and compiled. The dedicated controller, reader, and migration contract tests from the brief remain follow-up work.

## Changes

- Added shared tenant image read/write contracts in `ai-contracts` so assistant does not depend on catalog.
- Extended catalog image storage with tenant-scoped reads and path traversal protection.
- Added `CatalogDesignImageReader`, using the authenticated `AiExecutionContext` tenant.
- Added multipart `/api/ai/quotes` submission boundary with content-type and 10 MiB validation.
- Added quote request/response records and metadata repository port.
- Added durable `ai_design_image` metadata migration with RLS and changelog registration.

## Verification

- `mise exec java@25.0.2 -- ./gradlew :modules:assistant:test :modules:assistant:integrationTest :modules:catalog:test :database:test :modules:assistant:spotlessCheck --quiet` — passed.
- Initial compile caught and corrected an invalid assistant-to-catalog dependency; the final compile passed.

## Concerns

- `DesignImageMetadataRepository` is a port only; the controller does not yet persist metadata before invoking the quote use case.
- Dedicated `DesignQuoteControllerTest`, `CatalogDesignImageReaderTest`, and `AiDesignImageMigrationContractTest` were not added in this pass.
- The endpoint uses the existing synchronous quote use case; asynchronous job submission belongs to the planned job phase.
- Existing unrelated identity/subscriptions/tenancy working-tree changes were preserved.

## Concern resolution

- Added `JdbcDesignImageMetadataRepository`; metadata is persisted with tenant/workflow uniqueness and PostgreSQL RLS.
- Wired metadata persistence into the submission boundary after tenant-scoped storage.
- Added `DesignQuoteControllerTest`, `CatalogDesignImageReaderTest`, and `AiDesignImageMigrationContractTest`.
- Synchronous quote invocation remains deliberately bounded to this task; asynchronous orchestration is deferred to the planned job phase.

## Additional verification

- `mise exec java@25.0.2 -- ./gradlew :modules:assistant:test --tests '*DesignQuoteControllerTest' --tests '*CatalogDesignImageReaderTest' :database:test --tests '*AiDesignImageMigrationContractTest' :modules:assistant:integrationTest :modules:catalog:test :modules:assistant:spotlessApply :modules:assistant:spotlessCheck --quiet` — passed.
