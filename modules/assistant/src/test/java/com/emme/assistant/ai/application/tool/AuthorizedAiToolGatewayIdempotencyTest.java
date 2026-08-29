package com.emme.assistant.ai.application.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.assistant.ai.application.port.out.AiToolIdempotencyStore;
import com.emme.assistant.ai.application.trace.NoopAiTraceRecorder;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AuthorizedAiToolGatewayIdempotencyTest {

  @Test
  void returnsTheDurableMutationResultWithoutInvokingTheHandlerAgain() {
    AtomicInteger executions = new AtomicInteger();
    InMemoryToolIdempotencyStore store = new InMemoryToolIdempotencyStore();
    AuthorizedAiToolGateway gateway =
        new AuthorizedAiToolGateway(
            Set.of(
                new AiToolDefinition(
                    "createAppointment",
                    "Create an appointment",
                    Set.of("client"),
                    AiToolRisk.MUTATION,
                    true,
                    false,
                    (context, arguments) -> {
                      executions.incrementAndGet();
                      return "appointment-created";
                    })),
            NoopAiTraceRecorder.INSTANCE,
            store);
    AiExecutionContext context = context();

    AiToolResult first =
        AiExecutionContextScope.call(
            context,
            () ->
                gateway.execute(new AiToolInvocation("createAppointment", Map.of(), true, false)));
    AiToolResult replay =
        AiExecutionContextScope.call(
            context,
            () ->
                gateway.execute(new AiToolInvocation("createAppointment", Map.of(), true, false)));

    assertThat(first).isEqualTo(replay);
    assertThat(executions).hasValue(1);
    assertThat(store.claimedKeys)
        .containsExactly(
            "createAppointment:" + context.principalId() + ":" + context.idempotencyKey());
  }

  @Test
  void releasesTheMutationClaimWhenTheHandlerFailsSoTheCommandCanBeRetried() {
    AtomicInteger executions = new AtomicInteger();
    InMemoryToolIdempotencyStore store = new InMemoryToolIdempotencyStore();
    AuthorizedAiToolGateway gateway =
        new AuthorizedAiToolGateway(
            Set.of(
                new AiToolDefinition(
                    "createAppointment",
                    "Create an appointment",
                    Set.of("client"),
                    AiToolRisk.MUTATION,
                    true,
                    false,
                    (context, arguments) -> {
                      if (executions.incrementAndGet() == 1) {
                        throw new IllegalStateException("appointment service unavailable");
                      }
                      return "appointment-created";
                    })),
            NoopAiTraceRecorder.INSTANCE,
            store);
    AiExecutionContext context = context();
    AiToolInvocation invocation = new AiToolInvocation("createAppointment", Map.of(), true, false);

    assertThatThrownBy(
            () -> AiExecutionContextScope.call(context, () -> gateway.execute(invocation)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("appointment service unavailable");
    AiToolResult retry = AiExecutionContextScope.call(context, () -> gateway.execute(invocation));

    assertThat(retry).isEqualTo(new AiToolResult("createAppointment", "appointment-created", true));
    assertThat(executions).hasValue(2);
    assertThat(store.claimedKeys)
        .containsExactly(
            "createAppointment:" + context.principalId() + ":" + context.idempotencyKey(),
            "createAppointment:" + context.principalId() + ":" + context.idempotencyKey());
  }

  @Test
  void rejectsAConcurrentMutationWhenAnotherExecutionOwnsTheIdempotencyKey() {
    InMemoryToolIdempotencyStore store = new InMemoryToolIdempotencyStore();
    AiExecutionContext context = context();
    String operationKey =
        "createAppointment:" + context.principalId() + ":" + context.idempotencyKey();
    store.inProgress.add(operationKey);
    AuthorizedAiToolGateway gateway =
        new AuthorizedAiToolGateway(
            Set.of(
                new AiToolDefinition(
                    "createAppointment",
                    "Create an appointment",
                    Set.of("client"),
                    AiToolRisk.MUTATION,
                    true,
                    false,
                    (toolContext, arguments) -> "appointment-created")),
            NoopAiTraceRecorder.INSTANCE,
            store);

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    context,
                    () ->
                        gateway.execute(
                            new AiToolInvocation("createAppointment", Map.of(), true, false))))
        .isInstanceOf(AiToolExecutionRejectedException.class)
        .hasMessage("AI tool mutation is already in progress: createAppointment");
  }

  @Test
  void preservesTheOriginalMutationFailureWhenClaimCleanupFails() {
    InMemoryToolIdempotencyStore store = new InMemoryToolIdempotencyStore();
    store.releaseFailure = new IllegalStateException("idempotency store unavailable");
    AuthorizedAiToolGateway gateway =
        new AuthorizedAiToolGateway(
            Set.of(
                new AiToolDefinition(
                    "createAppointment",
                    "Create an appointment",
                    Set.of("client"),
                    AiToolRisk.MUTATION,
                    true,
                    false,
                    (context, arguments) -> {
                      throw new IllegalStateException("appointment service unavailable");
                    })),
            NoopAiTraceRecorder.INSTANCE,
            store);

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    context(),
                    () ->
                        gateway.execute(
                            new AiToolInvocation("createAppointment", Map.of(), true, false))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("appointment service unavailable")
        .hasSuppressedException(store.releaseFailure);
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("client"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-tool-idempotency",
        "request-1");
  }

  private static final class InMemoryToolIdempotencyStore implements AiToolIdempotencyStore {

    private final java.util.Map<String, AiToolResult> results = new java.util.HashMap<>();
    private final java.util.List<String> claimedKeys = new java.util.ArrayList<>();
    private final java.util.Set<String> inProgress = new java.util.HashSet<>();
    private RuntimeException releaseFailure;

    @Override
    public Optional<AiToolResult> find(String operationKey) {
      return Optional.ofNullable(results.get(operationKey));
    }

    @Override
    public boolean claim(String operationKey, String toolKey) {
      claimedKeys.add(operationKey);
      return !results.containsKey(operationKey) && inProgress.add(operationKey);
    }

    @Override
    public void complete(String operationKey, AiToolResult result) {
      results.put(operationKey, result);
      inProgress.remove(operationKey);
    }

    @Override
    public void release(String operationKey) {
      if (releaseFailure != null) {
        throw releaseFailure;
      }
      results.remove(operationKey);
      inProgress.remove(operationKey);
    }
  }
}
