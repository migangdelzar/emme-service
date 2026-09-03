# Canonical Chat Implementation Review

| Field | Detail |
|---|---|
| Review range | `45a53702..85e24eb2` |
| Target commit | `85e24eb2` (`feat(assistant): canonicalize chat composition`) |
| Branch | `feat/ai-platform-foundation` |
| Date | 2026-09-03 |
| Recommendation | **Approved** |

## Executive assessment

The reviewed slice implements the plan’s canonical ordinary-chat boundary. `ChatService`
now requires one `ChatCompletionPort` and no longer owns an `AiModelProvider` fallback.
The HTTP chat route, durable conversation workflow, and WhatsApp chat flow all reach
that service through `ChatUseCase`.

The Spring AI and legacy compatibility composition roots are mutually exclusive by the
`app.ai.spring-chat.enabled` property. Both preserve the existing selector and
unavailable-provider policy; both attach model admission and durable tracing at the
composition boundary. Backend AI context remains required before chat execution.

No High, Medium, or Low findings were identified for this implementation range.

## Findings

None. High: 0. Medium: 0. Low: 0.

## Review checks

### Canonical completion port

- `ChatService` has one required `ChatCompletionPort` dependency and invokes only that
  boundary for model completion.
- The old `ChatService` direct `AiModelProvider.chat(...)` fallback and its service-owned
  scheduler/admission state are removed.
- `AiController` routes ordinary `/api/ai/chat` requests to `ChatUseCase`; durable
  conversation processing and WhatsApp processing also depend on that use case.
- The separate RAG completion fallback in `RagQueryService` remains intentionally outside
  this slice, as stated in the plan and checkpoint report. It is not an ordinary chat
  composition path and is correctly documented as a later migration.

### Policy, admission, tracing, and tenant behavior

- Spring providers are adapted through `ChatProviderFailurePolicy`, so only
  `ChatProviderUnavailableException` reaches `ChatModelSelector` fallback.
- The compatibility root applies the same failure policy, wraps attempts in
  `TracingChatCompletionPort`, and routes them through `ChatModelSelector` with
  `ModelCapability.GENERATION` admission.
- The Spring root retains tenant and prompt advisors, tool callback integration, selector
  ordering, scheduler admission, and durable tracing.
- `ChatModelSelector` requires `AiExecutionContextScope` when admission is enabled; the
  service itself also fails closed without backend AI context.
- Tracing records both successful and failed provider attempts and remains best effort
  without changing completion semantics.

### API and scope regression check

- `ChatUseCase`, controller mappings, request/response types, and production chat callers
  are unchanged by the range.
- The removed `ChatService` constructors accepted the old legacy dependency shape; this is
  the intentional internal migration required by the approved plan. No production caller
  relies on those constructors.
- The range is limited to `ChatService`, the two chat composition roots, related tests,
  and the checkpoint report. No unrelated dirty-worktree files were staged.

### Tests and verification

Focused canonical assistant tests passed:

```text
mise exec -- ./gradlew :modules:assistant:test \
  --tests 'com.emme.assistant.ai.application.service.ChatServiceTest' \
  --tests 'com.emme.assistant.ai.application.service.ChatServiceProviderFallbackTest' \
  --tests 'com.emme.assistant.ai.application.service.ChatModelSelectorTest' \
  --tests 'com.emme.assistant.ai.application.provider.TracingChatCompletionPortTest' \
  --tests 'com.emme.assistant.ai.configuration.SpringAiChatConfigurationTest' \
  --tests 'com.emme.assistant.ai.configuration.LegacyChatCompletionConfigurationTest' \
  --tests 'com.emme.assistant.ai.configuration.ChatCompositionArchitectureTest' \
  --no-daemon
```

Result: **BUILD SUCCESSFUL**; 50 selected tests passed.

Relevant ai-platform adapter/configuration tests also passed:

```text
mise exec -- ./gradlew :modules:ai-platform:test \
  --tests 'com.emme.ai.platform.adapter.out.provider.springai.SpringAiChatModelTest' \
  --tests 'com.emme.ai.platform.adapter.out.provider.springai.SpringAiModelProviderTest' \
  --tests 'com.emme.ai.platform.configuration.AiProviderConfigurationTest' \
  --tests 'com.emme.ai.platform.configuration.AiProviderConfigurationIntegrationTest' \
  --no-daemon
```

Result: **BUILD SUCCESSFUL**.

A full assistant test run from a clean detached worktree at `85e24eb2` completed 456
tests with 18 failures. Those failures are the documented baseline failures in unchanged
package metadata, tenant JDBC configuration, and database-backed application-context
tests; none is in the reviewed chat implementation. The checkpoint report also documents
the broader limitation. `git show --check 85e24eb2` is clean.

## Positive observations

- The architecture regression test prevents the service from reacquiring the legacy
  provider fallback.
- Configuration tests cover disabled/missing and enabled Spring-chat profiles, including
  mutual exclusion of the compatibility bean.
- Selector tests retain ordered fallback, provider identity, admission-per-attempt, and
  non-fallback error behavior.
- The compatibility adapter preserves legacy provider availability while moving its
  execution through the canonical application port and shared policy boundaries.

## Deferred scope recorded, not a finding

Embedding, image captioning, and RAG consolidation remain separate migrations. In
particular, `RagQueryService` still contains its compatibility `AiModelProvider` path by
design; removing it here would exceed the approved canonical-chat slice.
