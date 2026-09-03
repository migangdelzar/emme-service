# Canonical Chat Path Checkpoint

**Date:** 2026-09-03
**Branch:** `feat/ai-platform-foundation`
**Status:** Checkpointed, incomplete

## Scope

The approved slice is to make assistant chat depend on one required
`ChatCompletionPort`, remove `ChatService`'s direct `AiModelProvider` fallback,
and provide a mutually exclusive compatibility composition path that preserves
the existing selector, admission, and durable tracing behavior.

## Exact verification result

Command run with the repository's Java 25 toolchain:

```text
mise exec -- ./gradlew :modules:assistant:test --tests 'com.emme.assistant.ai.application.service.ChatServiceTest' --tests 'com.emme.assistant.ai.application.service.ChatServiceProviderFallbackTest' --no-daemon
```

Result: **failed at `:modules:assistant:compileTestJava`** with exit code 1.
The twelve compilation errors all reported that `ChatCompletionPort` could not
be converted to the current `ChatService` constructor's `AiModelProvider`
parameter. This was the expected red result after updating the regression tests
first.

After that red result, `ChatService` was changed to require a `ChatCompletionPort`
and no longer contain the legacy provider fallback. A first compile exposed a
duplicate constructor; that source error was corrected, but the focused tests
were intentionally **not rerun** after the correction at the user's request.

## Current incomplete work

- `ChatService` production code now has the required canonical port shape, but
  its focused tests have not yet reached a green run.
- The existing service tests were migrated from direct `AiModelProvider.chat`
  calls to `ChatCompletionPort.complete`.
- The provider-fallback regression test now asserts that chat-port provider
  unavailability is propagated and does not invoke an unrelated legacy model.
- The compatibility composition configuration has not yet been implemented.
- Spring chat bean return types/conditional wiring have not yet been updated.
- The architecture/configuration regression test has not yet been added.
- No Java 25 focused test suite or full build result exists for this checkpoint.

## Checkpoint files

- `modules/assistant/src/main/java/com/emme/assistant/ai/application/service/ChatService.java`
- `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ChatServiceTest.java`
- `modules/assistant/src/test/java/com/emme/assistant/ai/application/service/ChatServiceProviderFallbackTest.java`
- `docs/superpowers/plans/2026-09-03-canonical-chat-path.md`

The unrelated pre-existing dirty-worktree changes were not staged.
