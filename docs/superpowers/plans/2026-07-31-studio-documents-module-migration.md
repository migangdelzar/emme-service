# Studio Documents Capability Migration Plan

> **For agentic workers:** Use the executing-plans or subagent-driven-development workflow. This capability is intentionally separate from the completed Studio core migration.

**Goal:** Migrate Studio Documents from nested legacy `entity`, `application`,
and `web` packages to the current module template while preserving document
status transitions, tenant ownership, chunk persistence, and HTTP behavior.

## Current inventory

```text
com.emme.studio.documents
├── application/DocumentService.java
├── entity/{Document,DocumentChunk,DocumentStatus,repositories}
└── web/DocumentController.java
```

## Target ownership

```text
com.emme.studio.documents
├── api/{command,query,result,usecase,exception,type}
├── application/{service,port/out,mapper}
├── domain/{model,exception}
├── adapter/in/web/{controller,request,response,mapper,advice}
├── adapter/out/persistence/{entity,repository,adapter,mapper,projection}
└── configuration
```

Only real document/chunk contracts are materialized. Embedding/search capability
is added under the owning outbound port/adapter, not as a generic helper package.

## Tasks

- [ ] Inventory HTTP consumers, Studio callers, schema/migrations, search callers,
  and document status/event behavior.
- [ ] Add red package/layer rules and pure status-transition tests.
- [ ] Extract framework-free `Document`/`DocumentChunk` domain models and status
  rules; move JPA classes to `DocumentEntity`/`DocumentChunkEntity`.
- [ ] Split repositories into `application/port/out`, Spring Data repositories,
  persistence adapters, mappers, and projections.
- [ ] Define grouped commands, queries, results, use cases, and exceptions only
  for existing document operations; do not invent upload/processing endpoints.
- [ ] Move `DocumentService` to application services and `DocumentController` to
  inbound web adapters with dedicated DTOs/mappers.
- [ ] Isolate embedding/search calls behind ports and provider adapters if the
  current implementation has a direct technical dependency.
- [ ] Add package-info to each materialized package and update Studio Modulith
  metadata without weakening existing module rules.
- [ ] Run focused Documents tests, Studio tests, integration tests, architecture,
  service CI, and migration/schema comparison.
- [ ] Record completion in the Studio plan and registry.

## Definition of done

- [ ] Documents is independently canonical and no nested legacy implementation
  package remains.
- [ ] Domain models are framework-free and entities never reach API/web code.
- [ ] Existing document status, tenant, persistence, and response behavior passes
  regression tests.
