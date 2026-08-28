# AI Dependency Compatibility Baseline

**Date:** 2026-08-27
**Status:** Pinned baseline; framework API adoption remains incremental

## Decision

The service pins Spring AI `2.0.1` and LangGraph4j `1.8.25` through the shared
Gradle platform. Spring AI is used for model, embedding, advisor, vector-store,
and MCP integration. LangGraph4j is used only for durable workflow
orchestration and checkpoint/resume behavior. Domain and application business
rules remain independent of both libraries.

The LangGraph4j `1.9.0-beta3` line is not selected for the production baseline
because it is a pre-release. It may be evaluated in an isolated compatibility
branch when a required feature cannot be implemented on the stable line.

## Provider posture

Spring AI supports multiple model implementations in one application. Emme
will expose provider-neutral application ports and create specialized Spring AI
clients per capability. Provider selection and fallback remain configuration
and policy decisions; no domain service will depend on one provider starter.

The initial production direction is:

- Ollama for local development and the Apple Silicon worker.
- OpenAI-compatible or other cloud providers only through an explicit tenant
  policy and privacy-approved fallback.
- pgvector through PostgreSQL for durable semantic references and knowledge.
- Redis for temporary operational state, never as the durable semantic source
  of truth.

## Compatibility verification

The baseline is verified by Gradle BOM resolution and module compilation before
framework APIs are imported. Provider credentials and paid model calls are not
required for the verification lane.

Authoritative references checked on 2026-08-27:

- [Spring AI getting started and dependency management](https://docs.spring.io/spring-ai/reference/getting-started.html)
- [Spring AI PGvector integration](https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html)
- [Spring AI MCP client](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html)
- [LangGraph4j README and stable installation guidance](https://github.com/langgraph4j/langgraph4j#installation)
- [LangGraph4j releases](https://github.com/langgraph4j/langgraph4j/releases)

## Constraints

- Do not import `spring-ai-starter-model-*` into domain packages.
- Do not let Spring AI `ChatMemory` replace PostgreSQL conversation history or
  audit records.
- Do not configure both LangGraph4j and Spring AI as competing workflow/tool
  loop owners.
- Re-run dependency resolution after every Spring Boot, Spring AI, or
  LangGraph4j version change.
