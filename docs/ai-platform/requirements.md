# Functional Requirements: Emme AI Platform

| Field | Value |
|---|---|
| Status | Draft |
| Parent | [PRD](PRD.md) |
| Technical reference | [Technical specification](technical-specification.md) |

## 1. Java and execution foundation

### REQ-AI-001 — Java 25 baseline

**Given** any supported local, CI, JVM-container, or test execution path
**when** the Emme service is built or started **then** Java 25 is selected and
the build fails with an actionable message when the runtime is incompatible.

### REQ-AI-002 — Context integrity

**Given** an authenticated request or trusted worker event **when** an AI
workflow starts **then** the backend creates an immutable execution context
containing tenant, principal, roles, conversation, workflow, trace, and
idempotency identifiers.

### REQ-AI-003 — Structured parallelism

**Given** independent read-only subtasks **when** a workflow executes them in
parallel **then** it uses the configured parallel-task abstraction, propagates
the execution context, applies a deadline, and joins according to an explicit
Joiner policy.

## 2. Semantic classification

### REQ-AI-010 — Deterministic-first routing

**Given** a user request **when** an explicit UI command or deterministic rule
matches confidently **then** the system routes without an LLM classification
call.

### REQ-AI-011 — Vector intent routing

**Given** no deterministic route **when** an embedding is generated with the
active index model/version **then** the system searches tenant/global intent
references and records top-1 score, top-2 score, margin, matched references,
missing slots, and index version.

### REQ-AI-012 — Abstention

**Given** a score, margin, slot, authorization, or index-integrity failure
**when** route policy is evaluated **then** the system abstains and uses
clarification or structured LLM fallback instead of guessing.

## 3. Semantic tool calling

### REQ-AI-020 — Approved tool selection

**Given** a classified intent **when** tools are searched **then** only tools
allowed by tenant capability, role, channel, risk policy, and workflow context
are eligible.

### REQ-AI-021 — Direct read-only execution

**Given** a high-confidence tool candidate with complete validated slots and a
read-only risk policy **when** execution is requested **then** the backend calls
the application use case directly and records an audit event.

### REQ-AI-022 — Mutation protection

**Given** a write tool such as booking or cancellation **when** the request is
not authorized, confirmed, or idempotency-safe **then** the system does not
execute the mutation.

## 4. Semantic caching

### REQ-AI-030 — Cache lookup

**Given** an incoming request **when** a semantically similar cache record is
searched **then** the system validates tenant/user scope, privacy class,
freshness, dependency versions, embedding version, score, and margin before
returning it.

### REQ-AI-031 — Cache eligibility

**Given** a completed response **when** cache eligibility is evaluated **then**
only safe read-only, non-sensitive, version-compatible responses are eligible
for online cache enrichment.

### REQ-AI-032 — Cache invalidation

**Given** a tenant policy, service, price, or quote-template version change
**when** a cached response depends on that version **then** the response is
bypassed or invalidated.

## 5. Spring AI and providers

### REQ-AI-040 — Specialized clients

The platform provides separate extraction, RAG, fallback-agent, response, and
embedding capabilities. No domain service depends directly on a provider SDK.

### REQ-AI-041 — Provider fallback

**Given** a configured local or cloud provider **when** the selected provider
times out or fails **then** fallback is allowed only by tenant privacy policy,
task policy, timeout budget, and retry policy.

### REQ-AI-042 — Structured output

**Given** model-extracted slots or design attributes **when** schema validation
fails **then** the result is repaired with bounded retries or routed to
clarification/HITL; invalid output is never used as business truth.

## 6. LangGraph4j workflow and HITL

### REQ-AI-050 — Durable workflow

The workflow persists state transitions and checkpoints in PostgreSQL and can
resume using a trusted workflow and conversation reference after restart.

### REQ-AI-051 — Approval state

**Given** an ambiguous design or policy-required decision **when** the approval
gate is reached **then** the workflow persists a review task, notifies the
authorized staff channel, pauses, and resumes only after an authenticated
approval or edit.

### REQ-AI-052 — Optimistic review locking

**Given** two staff members open the same review task **when** one submits a
decision **then** the other cannot overwrite the newer version.

## 7. Learning and observability

### REQ-AI-060 — Trace persistence

Every AI execution persists tenant, conversation, workflow, model, prompt,
embedding, route, tool, retrieval, validation, outcome, latency, token, and cost
metadata subject to redaction policy.

### REQ-AI-061 — Candidate enrichment

**Given** strong success evidence **when** an execution finishes **then** the
system may create a redacted candidate record and asynchronously generate its
embedding, but must not mutate the production routing index immediately.

### REQ-AI-062 — Promotion controls

Candidates are evaluated against regression and safety data, canaried in a
versioned index, monitored, and promoted or rolled back according to policy.
