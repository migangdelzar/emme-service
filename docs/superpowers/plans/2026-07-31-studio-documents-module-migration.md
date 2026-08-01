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
- [x] Define grouped commands, queries, results, and exceptions only for
  existing document operations; do not invent upload/processing endpoints.
- [x] Replace the multi-use-case `DocumentService` façade with one focused
  application service per use case and move `DocumentController` to inbound web
  adapters with dedicated DTOs/mappers.
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

## Completed domain and persistence boundary slice — 2026-08-01

- [x] Added framework-free `Document`, `DocumentChunk`, and `DocumentStatus`
  models with lifecycle invariants and focused tests.
- [x] Renamed JPA representations to `DocumentEntity` and
  `DocumentChunkEntity` under outbound persistence.
- [x] Added application-owned `DocumentRepository`, persistence mapper, Spring
  Data repositories, and persistence adapter.
- [x] Changed the document application service to depend on the outbound port,
  not JPA repositories or entities.
- [x] Extracted HTTP request/response records and mapper, and moved the
  controller to the canonical inbound adapter package.
- [x] Added package metadata and executable source/dependency boundary tests.
- [x] Verified focused document unit/module tests, compilation, formatting, and
  whitespace.

## Completed public contract and focused service slice — 2026-08-01

- [x] Added grouped command, query, result, exception, and use-case contracts.
- [x] Added one application service per document use case and removed the
  multi-use-case `DocumentService` façade.
- [x] Updated the inbound controller to depend directly on focused use-case
  ports and moved the web test into the canonical adapter package.
- [x] Exposed only `documents-api` through Spring Modulith's named interface.
- [x] Added source-tree regression coverage preventing the façade and multiple
  use-case implementations from returning.
- [x] Verified focused Documents tests, formatting, compilation, and whitespace.

The remaining Documents work is search and embedding port ownership, full Studio
integration evidence, and final Modulith verification.
  regression tests.
