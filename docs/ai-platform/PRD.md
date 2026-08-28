# Product Requirements Document: Emme AI Platform

| Field | Value |
|---|---|
| Product | Emme Nails |
| Status | Draft |
| Source architecture | [Master design](../superpowers/specs/2026-08-27-ai-platform-semantic-architecture-design.md) |

## 1. Problem

Emme needs an assistant that can understand salon requests, analyze nail
designs, explain services, assist with appointments, and help staff without
making authoritative business decisions. Unnecessary LLM calls increase cost,
latency, and variability. Existing AI code has provider and embedding support,
but routing is primarily model-driven and lacks durable workflow, HITL, and
governed semantic learning.

## 2. Product vision

Provide a fast, tenant-safe AI platform that uses deterministic rules and
semantic search for common requests, Spring AI for bounded model capabilities,
and LangGraph4j for durable multi-step workflows requiring state, retries, or
human approval.

## 3. Users

| User | Need |
|---|---|
| Client | Understand services, obtain a design quote, check availability, and book safely |
| Nail artist/staff | Review ambiguous designs, correct extracted attributes, and assist clients |
| Salon owner | Configure policies, prices, AI controls, reports, and promotions |
| Platform operator | Monitor providers, workflows, tenants, cost, and failures |

## 4. Goals

- Reduce avoidable model calls with semantic classification and caching.
- Route common read-only tools proactively when confidence and policy permit.
- Keep tenant, user, role, price, availability, and appointment authority in the
  backend.
- Support local Ollama/MLX-compatible models and optional cloud fallback.
- Make quotes explainable, deterministic, reviewable, and auditable.
- Learn from confirmed outcomes without poisoning production indexes.
- Provide durable workflow recovery after process restarts.

## 5. Scope

### Initial platform scope

- Java 25 repository baseline.
- Structured concurrency with `ScopedValue`, `StructuredTaskScope`, and Joiners.
- Named virtual-thread and bounded executors.
- Spring AI provider abstraction and specialized clients.
- LangGraph4j workflow and PostgreSQL checkpoint persistence.
- Semantic classification, semantic tool selection, and semantic caching.
- Nail-design extraction, deterministic quote calculation, and HITL review.
- Tenant/user-scoped conversation, workflow, and evaluation traces.
- Online candidate enrichment and controlled index promotion.
- Observability and asynchronous evaluation scaffolding.

### Later scope

- WhatsApp production adapter hardening.
- Reports, recommendations, promotions, and campaign drafts.
- Optional Neo4j/Apache AGE relationship retrieval.
- Redis vector search after an infrastructure compatibility decision.

## 6. Non-goals

- A separate deployable AI microservice in the initial release.
- LLM-owned prices, appointments, permissions, or tenant selection.
- Unrestricted generated SQL or Cypher.
- Fully autonomous production model or tool changes.
- Synchronous Ragas evaluation during a customer request.

## 7. Success measures

Initial targets are calibration goals and must be measured against baseline:

- Reduced LLM calls for high-frequency read-only intents.
- Sub-second semantic cache hits under target load.
- Zero confirmed cross-tenant retrievals.
- Zero duplicate appointment writes caused by retries.
- Measurable reduction in false tool routes after calibration.
- Quote correction rate visible and decreasing without sacrificing safety.
- Workflow recovery after worker restart without data loss.
- Per-tenant AI usage and cost visible to operators.

## 8. Product guardrails

- A vector match is a candidate decision, not business authority.
- A model extraction is untrusted until schema and domain validation pass.
- Mutations require authorization, confirmation where applicable, idempotency,
  audit, and application use-case execution.
- Online enrichment records candidates; production promotion is versioned,
  evaluated, canaried, and reversible.
