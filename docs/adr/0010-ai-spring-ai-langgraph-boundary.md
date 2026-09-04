# ADR-0010: Spring AI and LangGraph4j Boundary

## Status

Accepted

## Date

2026-08-27

## Context

Spring AI provides model, embedding, advisor, vector, MCP, and tool-call
integration. LangGraph4j provides graph orchestration, state, checkpoints,
conditional routing, and pause/resume. Using both without clear ownership can
create two competing tool loops or duplicate memory/state.

## Decision

- Spring AI owns model/provider and tool-callback interactions.
- LangGraph4j owns the outer workflow and HITL lifecycle.
- Spring AI `ToolCallingAdvisor` owns one fallback-agent tool loop.
- Direct semantic tool matches call application use cases without an LLM loop.
- Complete conversation history and workflow checkpoints remain in PostgreSQL.
- Spring AI ChatMemory is a prompt-context adapter, not the source of truth.
- MCP is used only for external or independently deployable integrations.

## Consequences

- Responsibilities are testable and observable separately.
- The graph can complete common requests without an LLM.
- Tool authorization remains in application use cases.
- LangGraph4j and Spring AI versions require an explicit compatibility spike.
- Enabling the durable conversation graph without real capability adapters fails
  at startup; placeholder defaults are limited to isolated graph tests.
- A successful durable workflow owns and returns the final response, preventing
  a second chat-model execution in the application service.
