# AI Dependency Compatibility Baseline

**Date:** 2026-09-04
**Status:** Pinned baseline; runtime integration validation pending local infrastructure

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

The optional `langgraph4j-spring-ai` bridge is intentionally not part of the
runtime dependency graph yet. The application uses Spring AI for model,
advisor, retrieval, and tool mechanics, while LangGraph4j core owns only the
durable state graph and checkpoint lifecycle. Adding the bridge would be
appropriate only when graph nodes need its Spring AI-specific agent/tool
services; adding it now would create a second tool-loop owner and increase
coupling without providing a required capability.

## Tool-loop boundary

The current bounded-agent path is Spring AI's `ToolCallingAdvisor`. Emme's
`AuthorizedAiToolGateway.agentEligibleToolDefinitions()` is the allow-list
boundary: it exposes only backend-authorized, read-only tools that require
neither user confirmation nor staff approval. The Spring AI callback provider
uses that set directly, so mutation tools never enter the model's tool schema.

Spring AI `2.0.1` supplies the single recursive tool loop. Its default
`DefaultToolCallingManager` limits a tool to 40 calls and a turn to 150 total
calls, and its default resolution fallback is disabled; requests can execute
only callbacks attached to that request. These limits are a bounded safety
default, not a license to expose unrestricted tools. If Emme needs tighter
limits or a generic ReAct sub-agent later, configure them at this boundary and
keep the sub-agent read-only and isolated from the durable business workflow.

References: [Spring AI tool calling](https://docs.spring.io/spring-ai/reference/api/tools.html),
[Spring AI ToolCallingAdvisor](https://docs.spring.io/spring-ai/reference/api/tools/tool-calling-advisor.html).

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

## Vision capability matrix

Image captioning remains on the existing provider-neutral `AiModelProvider`
contract and uses Spring AI's existing `ChatClient` multimodal `Media` support;
no second HTTP transport is introduced.

| Provider | Current vision behavior |
|---|---|
| Mock | Retains its deterministic-development placeholder caption behavior. |
| Ollama | Uses the configured Spring AI chat client for captioning when the selected Ollama model supports vision (the default Gemma 4 profile does). |
| Groq | Remains unchanged: the current OpenAI-compatible wiring is chat-only and does not configure a vision model. Caption requests fail explicitly as unsupported rather than returning a fabricated caption. |

The tenant-safe quote workflow remains on `SpringAiNailDesignExtractor`, which
loads image bytes through `DesignImageReader` under `AiExecutionContext` before
calling Spring AI. The base64 caption contract is used only by existing catalog
flows that already receive tenant-scoped image input.

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
- Run container-backed integration and deployed E2E validation before declaring
  a production runtime certification; source compilation and unit checks do not
  prove Docker/PostgreSQL/Kafka wiring.
