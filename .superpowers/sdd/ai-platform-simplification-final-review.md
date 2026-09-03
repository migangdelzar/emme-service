# AI Platform Simplification — Final Branch Review

**Review point:** `c47aab7bd6a68ea713339a090cbf7289c99b04c2`

**Branch:** `feat/ai-platform-foundation`

**Reviewed:** the simplification blueprint, implementation plan, progress ledger, committed `ai-platform`, `assistant`, `ai-contracts`, `kernel` context/security paths, application configuration, and the security configuration that protects the AI endpoints.

**Scope constraint:** review only. No production code was changed. Existing dirty-worktree changes were intentionally left untouched.

## Executive assessment

The branch successfully removes the raw OkHttp provider implementations and provider-owned intent routing. Spring AI is now present at the transport boundary, tenant-scoped retrieval/checkpoint wrappers exist, and the scheduler, durable jobs, idempotency, and trace redaction have meaningful tests.

It is not yet a completed simplification from an architecture perspective. The application still has a parallel legacy `AiModelProvider` path beside the assistant Spring AI selector path, two independent `app.ai` configuration models, and duplicate Spring AI chat/embedding adapters. Some framework delegation is therefore opt-in or bypassed in real execution paths. The quote upload endpoint also lacks an endpoint-level authorization/capability check and does not establish that its supplied conversation belongs to the authenticated principal.

**Recommendation:** do not treat this as the final architecture baseline. Fix the quote authorization boundary before enabling that feature, then land the small canonical-chat migration described below. Keep embedding, image captioning, and RAG consolidation as separate slices.

## What is solid

- Raw Ollama/Groq HTTP provider classes and the old provider HTTP helper are deleted; the active platform configuration constructs Spring AI clients/models.
- Intent routing no longer belongs to provider transport contracts or implementations.
- `TenantScopedDocumentRetriever`, `TenantAwareCheckpointSaver`, `DocumentKnowledgeRetrievalAdapter`, and the JDBC workflow/semantic adapters consistently bind reads to the backend `AiExecutionContext` and tenant predicate.
- Model admission is bounded by global, capability, tenant, user, and queue limits without creating an executor internally.
- Tool execution keeps authorization, confirmation, idempotency, and handler invocation in backend code; agent-visible callbacks expose only read-only eligible tools.
- Durable trace payloads are redacted before persistence, and Micrometer labels are bounded and avoid tenant/user cardinality.
- Learning remains persistence-backed, asynchronous, and promotion-gated according to the ledger.

## Prioritized findings

### F-01 — High: two model execution paths remain active and the legacy fallback bypasses the Spring AI policy stack

`ChatService` and `RagQueryService` accept an optional assistant `ChatCompletionPort`, but retain a required `AiModelProvider` and fall back to `provider.chat(...)` after a `ChatProviderUnavailableException`. `AiProviderConfiguration` still creates `SpringAiModelProvider`, while `SpringAiChatConfiguration` separately builds the Spring AI registry/selector. `DocumentKnowledgeRetrievalAdapter` has the same split for embeddings.

The legacy platform path uses `SpringAiChatModel` without the assistant's `TenantSecurityAdvisor`, prompt advisor, tool callback provider, or `TracingChatCompletionPort`. A failure in the selected Spring AI path can therefore cause a second call through a materially different policy and observability boundary, often to the same configured provider. This creates inconsistent prompt/tool/context behavior, duplicate provider work, and a route that is not covered by the same durable trace semantics.

**Evidence:**

- `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/ChatService.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/RagQueryService.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/out/persistence/DocumentKnowledgeRetrievalAdapter.java`
- `modules/ai-platform/src/main/java/com/emme/ai/platform/configuration/AiProviderConfiguration.java`
- `modules/ai-platform/src/main/java/com/emme/ai/platform/adapter/out/provider/springai/SpringAiModelProvider.java`

**Required direction:** make one canonical chat execution path authoritative. Migrate callers to the selector/application port, make fallback mean “next configured model” rather than “call the legacy provider again,” and retain `AiModelProvider` only for explicitly unmigrated capabilities until those callers are moved. Add an architecture test that rejects a legacy chat fallback from assistant services.

### F-02 — High: design-quote upload has no endpoint-level role/capability or conversation-ownership check

`DesignQuoteController` is authenticated only by the global `SecurityConfiguration` catch-all. Unlike `AiController` and `QuoteReviewController`, it has no `@PreAuthorize` expression or equivalent capability gate. `ProcessDesignQuoteService` creates/uses a workflow for the supplied `conversationId`, but does not load the conversation and verify that it belongs to the current tenant and principal. The persistence checks cover the AI workflow context, not ownership of the referenced assistant conversation.

An authenticated user who knows another conversation UUID in the same tenant can submit an image and attach quote state to that conversation. The absence of a feature/capability check also makes the controller's `app.ai.quote.enabled` switch the only application-level gate once enabled.

**Evidence:**

- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/web/controller/DesignQuoteController.java`
- `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/ProcessDesignQuoteService.java`
- `modules/identity/src/main/java/com/emme/identity/configuration/SecurityConfiguration.java` (`anyRequest().authenticated()`)
- `modules/assistant/src/main/java/com/emme/assistant/ai/adapter/in/web/controller/QuoteReviewController.java` (the stronger contrasting boundary)

**Required direction:** put authorization at the HTTP/application boundary and verify the referenced conversation through a tenant- and principal-scoped query before storing image metadata or creating a workflow. Add an IDOR regression test using two principals in one tenant and a feature/capability-denied test.

### F-03 — Medium: Spring AI chat and embedding translation is duplicated across `ai-platform` and `assistant`

`ai-platform` contains `SpringAiChatModel` and `SpringAiEmbeddingModel`; `assistant` contains `SpringAiChatClientAdapter` and `SpringAiEmbeddingAdapter`. The chat pair duplicates the system prompt, context concatenation, advisor application, tool callback handling, and tool-search session fingerprinting. The embedding pair duplicates text validation, provider invocation, dimension checks, and float conversion.

The duplication is not merely naming: the two chat implementations have different failure/trace behavior, and the assistant registry currently uses the assistant copy while the platform `AiModelProvider` uses the platform copy. Future fixes can silently land in only one path.

**Required direction:** define one framework-translation adapter owner (prefer `ai-platform` for Spring AI transport) and make assistant depend on a framework-neutral contract. Keep the small assistant-owned policy layer for tenant authorization, fallback, and deterministic validation; delete the duplicate translation only after caller and integration tests prove the single path is active.

### F-04 — Medium: Spring AI RAG retrieval is performed twice when the opt-in RAG path is enabled

`SpringAiRagConfiguration` puts `RetrievalAugmentationAdvisor` into the advisors used by `SpringAiChatProviderRegistry`. `RagAnswerProviderChain` also calls `KnowledgeSearch` directly, concatenates the returned documents, and then invokes that registry-backed completion. The same request consequently performs manual retrieval and advisor retrieval, potentially with different results and duplicated grounding text.

This adds vector/database and model-context cost and makes the abstention contract difficult to reason about: the manual path may reject an empty result while the advisor path is independently retrieving. It also means the framework capability is configured but not actually the sole owner of RAG mechanics.

**Required direction:** choose one RAG owner. Preserve tenant-scoped retrieval and explicit abstention as application policy, but either make the tenant retriever/advisor the only retrieval path or make the manual chain the only path. Test one retrieval invocation per request and empty-retrieval behavior.

### F-05 — Medium: `app.ai` has two independently bound configuration models

`AiProviderProperties` in `ai-platform` and `AiProperties` in `assistant` both bind the root `app.ai` namespace and independently define provider/chat/embedding defaults. The branch adds a test comparing selected embedding defaults, but the runtime still has two configuration objects and two composition roots. The default application configuration also leaves the assistant Spring AI chat, embedding, and RAG paths disabled, so the default runtime continues to exercise the legacy mock/provider stack.

This allows model name, version, dimension, base URL, and provider enablement to drift between the platform and assistant. A deployment can appear configured for Spring AI while a consumer silently uses the other object/path.

**Required direction:** establish one canonical root provider configuration and inject capability-specific derived settings into consumers. Keep feature flags for opt-in capabilities, but make the active chat/embedding source unambiguous and add a startup/architecture assertion for the selected composition root.

### F-06 — Medium: real-provider image captioning still resolves to the mock placeholder

`AiModelProvider.caption` has a default implementation that returns a random placeholder string. `SpringAiModelProvider` does not override it, while `AiCaptionImageAdapter` delegates directly to that method. Catalog services consume `CaptionImageUseCase`, so switching the provider away from mock does not cause real image captioning to be delegated to Spring AI; it silently stores fake captions.

**Evidence:**

- `libraries/ai-contracts/src/main/java/com/emme/ai/contracts/model/AiModelProvider.java`
- `modules/ai-platform/src/main/java/com/emme/ai/platform/adapter/out/provider/springai/SpringAiModelProvider.java`
- `modules/ai-platform/src/main/java/com/emme/ai/platform/adapter/out/capability/AiCaptionImageAdapter.java`
- `modules/catalog/src/main/java/com/emme/catalog/application/service/AddCatalogItemImageService.java`
- `modules/catalog/src/main/java/com/emme/catalog/application/service/MatchCatalogItemsService.java`

**Required direction:** separate image captioning from the combined provider contract. Until a real Spring AI vision adapter exists, fail explicitly when captioning is unavailable; never use a random placeholder in a real-provider path.

### F-07 — Low/Medium: observability ownership and loss semantics are not explicit

Spring AI observations are configured for provider calls, but assistant still wraps calls with `TracingChatCompletionPort` and `TracingEmbeddingModelPort` to persist custom durable traces. That durable trace is useful because it contains Emme-specific audit fields and redacted payloads, so it should not be deleted casually; however, the two observation systems have no documented field-ownership or failure signal. `SpringAiTraceConfiguration` falls back to `NoopAiTraceRecorder` when JDBC is absent, and the tracing wrappers swallow recorder failures. Missing durable traces can therefore be silent, while the branch's progress ledger records only focused test success rather than a runtime telemetry verification.

**Required direction:** document the split: Spring observations own provider latency/transport telemetry; the Emme recorder owns durable audit/business outcome records. Add a bounded counter/log event for recorder failures and a production profile check that durable tracing is configured where audit is required. Verify one successful call, one provider failure, and one recorder failure through actual telemetry.

## Remaining duplicated or intentionally custom abstractions

| Area | Current state | Keep, merge, or delegate |
|---|---|---|
| `AiModelProvider` | Combines chat, embedding, and captioning; still used as a fallback/compatibility boundary | Merge callers into capability ports; delete by capability after migration |
| Spring AI chat adapters | Near-duplicate implementations in platform and assistant | Keep one platform transport adapter; retain assistant policy/selector |
| Spring AI embedding adapters | Near-duplicate model wrappers plus a separate vector-store bridge | Keep one model transport adapter; retain only the vector-store bridge as a distinct framework adapter |
| Chat/RAG chains | Selector and manual `RagAnswerProviderChain` coexist with `RetrievalAugmentationAdvisor` | One completion path and one RAG owner |
| `AiProperties` / `AiProviderProperties` | Same `app.ai` root bound twice | Merge into one canonical provider configuration |
| `Tracing*Port` | Custom durable audit around Spring observations | Keep only for Emme audit fields; explicitly separate its ownership from Micrometer/OpenTelemetry |
| `TenantSecurityAdvisor`, tenant retriever, checkpoint saver, tool gateway | Application security policy around framework callbacks | Keep; these are policy boundaries, not framework replacements |
| `BoundedModelExecutionScheduler` | Custom tenant/user fairness and admission policy | Keep; framework executors do not provide this Emme policy |

## Next smallest implementation slice

### Slice: canonicalize the chat path only

This is the smallest high-leverage slice because it removes the most dangerous duplicate execution route without mixing embedding, image, RAG, or workflow changes.

1. Add failing tests proving `ChatService` uses the configured `IdentifiedChatCompletionPort`/selector and does not invoke `AiModelProvider.chat` on provider failure; preserve the existing fallback-order tests at the selector boundary.
2. Make `ChatCompletionPort` the required chat dependency of `ChatService`; remove only the chat-specific legacy constructors/fields/fallback. Leave `AiModelProvider` in place temporarily for the still-unmigrated embedding and captioning consumers.
3. Ensure `SpringAiChatConfiguration` is the sole assistant chat composition path when enabled, with tenant advisor, prompt version, tools, admission, and trace recorder attached once.
4. Add an architecture/configuration test that fails if assistant chat wiring contains a legacy provider fallback or if both chat composition roots are selected for one runtime profile.
5. Run the focused assistant/ai-platform tests and the application context test with Spring AI chat enabled; then document the remaining compatibility uses of `AiModelProvider`.

After that slice is green, migrate embedding (`DocumentKnowledgeRetrievalAdapter` and `AiEmbeddingAdapter`) as a separate slice. Then fix the RAG double-retrieval boundary and image captioning placeholder independently. The quote authorization finding is a prerequisite before enabling quote upload, not something to defer behind the simplification work.

## Verification notes

- Reviewed committed source and documents at `c47aab7b`, not the dirty working-tree versions.
- `git show --check c47aab7b` is clean for the reviewed commit.
- The progress ledger records focused tests and reviews as passing, but its final plan still leaves the blueprint-update and push checkboxes unchecked; the blueprint itself does not enumerate the actual removals or remaining compatibility paths. This report supplies that missing final inventory.
- No production code or unrelated dirty files were modified during this review.
