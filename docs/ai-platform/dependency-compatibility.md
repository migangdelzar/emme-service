# AI Dependency Compatibility Baseline

**Date:** 2026-08-27
**Status:** Pinned baseline; framework API adoption remains incremental

## Decision

The service pins Spring AI `2.0.1` and LangGraph4j `1.8.25` through the shared
Gradle platform. Spring AI is used for model, embedding, advisor, vector-store,
and MCP integration. LangGraph4j core is used only for durable workflow
orchestration and checkpoint/resume behavior. Emme provides its own JDBC
`BaseCheckpointSaver` adapter rather than using the optional stock PostgreSQL
saver as the authoritative store. Domain and application business rules remain
independent of both libraries.

The LangGraph4j `1.9.0-beta3` line is not selected for the production baseline
because it is a pre-release. It may be evaluated in an isolated compatibility
branch when a required feature cannot be implemented on the stable line.

## Provider posture

Spring AI supports multiple model implementations in one application. Emme
will expose provider-neutral application ports and create specialized Spring AI
clients per capability. Provider selection and fallback remain configuration
and policy decisions; no domain service will depend on one provider starter.
The assistant currently uses the direct `spring-ai-ollama` module and explicit
`OllamaEmbeddingModel` construction so the local provider is opt-in and named
provider beans can be composed without globally enabling a starter. Resolution
continues to select the repository’s Spring Boot `4.1.0` constraint.

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
- [LangGraph4j discussion on PostgreSQL saver cache behavior](https://github.com/langgraph4j/langgraph4j/discussions/356)

## Constraints

- Do not import `spring-ai-starter-model-*` into domain packages.
- Keep provider construction in infrastructure configuration; do not make the
  local Ollama model a required application dependency.
- Do not let Spring AI `ChatMemory` replace PostgreSQL conversation history or
  audit records.
- Do not configure both LangGraph4j and Spring AI as competing workflow/tool
  loop owners.
- Keep LangGraph checkpoint access behind the backend AI execution context; the
  LangGraph thread ID is an internal workflow ID, never a client-selected tenant
  selector.
- Re-run dependency resolution after every Spring Boot, Spring AI, or
  LangGraph4j version change.
