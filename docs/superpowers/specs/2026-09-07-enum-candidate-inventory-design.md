# Enum Candidate Inventory and Migration Design

| Field | Detail |
|---|---|
| Date | 2026-09-07 |
| Status | Draft for user review |
| Scope | All Java modules and shared libraries in `emme-service` |
| Compatibility | Preserve existing JSON, database, and messaging values exactly |

## 1. Objective

Create a complete, reviewable inventory of values that should be represented by
enums, then migrate those candidates in small contract-safe slices. The goal is
strong typing for closed internal vocabularies without forcing enums onto free
text, identifiers, tenant-configurable values, or extensible external data.

This is an audit and migration plan. It does not authorize implementation until
the candidate register and its sequencing have been reviewed.

## 2. Governing rule

Strongly type closed internal vocabularies. Normalize external values at
adapters. Preserve strings only at genuine serialization or extensibility
boundaries.

Use enums for lifecycle states, workflow states, actions, roles/scopes/actors,
closed channels, controlled event types, AI decisions/capabilities/routes, and
other finite values that drive business logic.

Keep `String` or use a value object for names, descriptions, messages, prompts,
URLs, tokens, IDs, slugs, codes, provider IDs, model versions, arbitrary JSON,
tenant-defined categories/roles, and other open-ended data. ISO currencies,
MIME types, and locales remain strings or value objects unless a specific
bounded application contract proves that an enum is appropriate.

## 3. Boundary and ownership model

Each vocabulary has one semantic owner:

| Vocabulary | Preferred owner |
|---|---|
| Domain lifecycle/business state | Owning module `domain.model` |
| Public API vocabulary | Owning module `api.type` |
| Provider-neutral cross-module contract | `libraries:kernel` or `libraries:ai-contracts` |
| Persistence-only vocabulary | Persistence adapter |
| External provider vocabulary | Provider adapter and its mapper |

Domain/application code must not convert finite values to `String` merely to
cross a mapper. Public records use API-owned enums when the public contract is
finite. Persistence uses stable enum names (`EnumType.STRING` or the equivalent)
and keeps existing stored values. JSON and message serialization retain the
existing names.

External adapters parse known values explicitly. Unknown external values must
not silently become a known business state. The candidate-specific design must
choose one of rejection/quarantine, an explicit raw-unknown result, or an
`UNKNOWN` value only where that fallback is behaviorally safe.

Identical enum names are not sufficient grounds for centralization. A vocabulary
is shared only when its meaning, lifecycle, and contract owner are genuinely
shared.

## 4. Candidate inventory method

The next session should inspect, in order:

1. All `src/main/java` and `src/test/java` files under `modules/*` and
   `libraries/*`.
2. SQL migrations, schema definitions, JPA entities, enum annotations, and
   repository queries.
3. Public API records, HTTP request/response DTOs, messaging events, and
   cross-module contracts.
4. Configuration properties and `@ConditionalOnProperty` values.
5. External adapter payload parsing, webhook handling, provider result mapping,
   and fixed protocol tokens.
6. Tests and fixtures that construct or assert the candidate values.

Search by semantic names (`status`, `state`, `type`, `kind`, `role`, `channel`,
`provider`, `source`, `mode`, `event`, `action`, `decision`, `outcome`, `scope`,
`plan`, `persona`, `route`, `capability`, `risk`, and `policy`) and by repeated
string literals. Every hit must be classified by meaning rather than by name.

The register must contain one row per vocabulary, not one row per occurrence,
with these fields:

| Field | Required content |
|---|---|
| Candidate ID | Stable identifier such as `ENUM-ASSISTANT-001` |
| Vocabulary | Proposed semantic name |
| Current locations | Source files, schema columns, events, or config keys |
| Current representation | Enum, string, literal comparison, or mixed |
| Owner | Module and layer |
| Proposed type | Existing enum, new enum, value object, or keep string |
| Values/source | Known values and authoritative source |
| Boundary impact | API, DB, message, config, adapter, or none |
| Unknown policy | Reject, quarantine, raw value, or safe `UNKNOWN` |
| Compatibility risk | Low/medium/high with rationale |
| Dependencies | Required preceding slices |
| Verification | Tests, architecture checks, and live gates |

## 5. Initial candidate families

These are starting families discovered during the repository reconnaissance;
the inventory must confirm, split, merge, or reject them with evidence.

### 5.1 Baseline already represented by enums

Do not reimplement these migrations. Verify their boundary coverage and record
them as completed baseline:

- Appointment, calendar, catalog, client, document, identity membership,
  notification, payment, service/artist, subscription, and tenant lifecycle
  statuses.
- Tenant provisioning state and payment workflow status.
- Assistant action/conversation statuses and action types.
- Existing AI workflow, quote, learning-candidate, model-capability, graph,
  guardrail, and semantic contract enums.
- Existing calendar persona/provider, identity provider/scope, salon day/policy,
  shared locale/search, and notification/channel enums.

### 5.2 Remaining or potentially leaking finite values

The next session must inspect these families explicitly:

- Assistant conversation `channel`, conversation `eventType`, pending-action
  action/status mapping, workflow/checkpoint status strings, quote-review
  decision persistence, workflow type, and operational outcome labels.
- AI contract delivery/output channel fields, workflow type, provider fields,
  and any duplicated `Channel` definitions between `kernel` and
  `ai-contracts`.
- Notification response channel and notification provider selection values.
- Payment provider selection, provider result status normalization, webhook
  provider/event values, and workflow-correlation provider fields.
- Subscription plan strings despite the existing `PlanType` contract enum.
- Identity customer-provider response fields, role values, and role-code
  boundaries; distinguish fixed platform roles from tenant-extensible roles.
- Appointment server-sent-event type values and any actor-role boundary not
  already covered by `AppointmentActor`.
- Calendar external protocol/persona values and provider status parsing where
  raw strings remain at the adapter edge.
- AI observability operation/outcome/scope labels, but only when the label set
  is a closed application contract rather than a metric implementation detail.
- Audit, webhook, job, and event classifications in modules where values drive
  branching or persistence state.
- Duplicate or overlapping shared types such as `Channel`,
  `ChannelType`, and `NotificationChannel`; resolve semantic ownership before
  proposing consolidation.

### 5.3 Explicit exclusions to validate

The register should normally exclude service categories, customer names,
provider IDs, model versions, role codes supplied by tenants, event payloads,
URLs, tokens, free-form reasons, descriptions, message bodies, metadata keys,
and arbitrary intent/parameter text. Exclusions must still be recorded with a
short rationale so the audit is complete and repeatable.

## 6. Migration sequencing

The implementation plan produced after this design should use these phases:

1. **Inventory and evidence** — build the candidate register and capture all
   current representations, values, owners, and consumers.
2. **Boundary regression guards** — add focused convention/contract tests for
   finite fields that must not regress to `String`.
3. **Existing enum leakage** — preserve existing enum ownership while replacing
   string fields in application, API, persistence, and message mappings.
4. **Missing internal enums** — add one owning enum at a time, including explicit
   conversion where domain and API contracts are intentionally separate.
5. **Shared contract cleanup** — resolve only genuinely shared vocabularies and
   avoid accidental cross-module coupling.
6. **External normalization** — add adapter mappers and unknown-value behavior
   for provider/webhook/protocol values.
7. **Persistence and compatibility verification** — confirm existing rows,
   event payloads, and JSON values remain unchanged.
8. **Final audit** — rerun semantic searches, architecture tests, module tests,
   formatting, and the full fast quality gate.

Each migration slice follows Red → Green → Refactor and must include affected
fixtures and consumers in the same compatibility change. No broad mechanical
replacement is allowed without semantic classification.

## 7. Definition of done for the inventory

- Every module and shared library has been audited.
- Every finite vocabulary has an owner and proposed representation.
- Every retained string has an explicit reason.
- Existing enum migrations are separated from remaining candidates.
- API, database, and messaging compatibility is stated for each candidate.
- Unknown external values have an explicit policy.
- Candidate dependencies and implementation order are documented.
- The resulting implementation plan names a test file and source area for each
  migration slice.
- No source code is changed as part of the inventory/design stage.

## 8. Next-session handoff

Start by syncing the active feature branch and reading this document together
with `tasks/todo.md`, `tasks/lessons.md`, and the existing enum-related commits.
Preserve unrelated working-tree changes. Complete the candidate register before
writing migration code. After the register is approved, create the detailed
execution plan with one TDD slice per accepted candidate family.
