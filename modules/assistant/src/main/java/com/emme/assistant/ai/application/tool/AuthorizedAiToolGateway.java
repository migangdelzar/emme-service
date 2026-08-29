package com.emme.assistant.ai.application.tool;

import com.emme.assistant.ai.application.trace.AiToolCallStatus;
import com.emme.assistant.ai.application.trace.AiToolCallTrace;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.assistant.ai.application.trace.NoopAiTraceRecorder;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** In-process tool gateway that keeps authorization and tenant context outside model control. */
public final class AuthorizedAiToolGateway implements AiToolGateway {

  private final Map<String, AiToolDefinition> definitions;
  private final AiTraceRecorder traceRecorder;

  public AuthorizedAiToolGateway(Collection<AiToolDefinition> definitions) {
    this(definitions, NoopAiTraceRecorder.INSTANCE);
  }

  public AuthorizedAiToolGateway(
      Collection<AiToolDefinition> definitions, AiTraceRecorder traceRecorder) {
    Objects.requireNonNull(definitions, "definitions must not be null");
    this.traceRecorder = Objects.requireNonNull(traceRecorder, "traceRecorder must not be null");
    Map<String, AiToolDefinition> byKey = new LinkedHashMap<>();
    definitions.forEach(
        definition -> {
          Objects.requireNonNull(definition, "tool definition must not be null");
          if (byKey.putIfAbsent(definition.key(), definition) != null) {
            throw new IllegalArgumentException("Duplicate AI tool definition: " + definition.key());
          }
        });
    this.definitions = Map.copyOf(byKey);
  }

  @Override
  public Set<String> proactivelyEligibleToolKeys() {
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    return definitions.values().stream()
        .filter(AiToolDefinition::canRunProactively)
        .filter(definition -> definition.isAuthorized(context.roles()))
        .map(AiToolDefinition::key)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public Set<AiToolDefinition> agentEligibleToolDefinitions() {
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    return definitions.values().stream()
        .filter(definition -> definition.risk() == AiToolRisk.READ_ONLY)
        .filter(definition -> !definition.userConfirmationRequired())
        .filter(definition -> !definition.staffApprovalRequired())
        .filter(definition -> definition.isAuthorized(context.roles()))
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public AiToolResult execute(AiToolInvocation invocation) {
    Objects.requireNonNull(invocation, "invocation must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    AiToolDefinition definition = definitions.get(invocation.toolKey());
    long startedAt = System.nanoTime();
    boolean authorized = definition != null && definition.isAuthorized(context.roles());
    try {
      if (definition == null) {
        throw new AiToolExecutionRejectedException("Unknown AI tool: " + invocation.toolKey());
      }
      if (!authorized) {
        throw new AiToolExecutionRejectedException(
            "AI tool is not authorized: " + invocation.toolKey());
      }
      if (definition.userConfirmationRequired() && !invocation.userConfirmed()) {
        throw new AiToolExecutionRejectedException(
            "User confirmation is required for AI tool: " + invocation.toolKey());
      }
      if (definition.staffApprovalRequired() && !invocation.staffApproved()) {
        throw new AiToolExecutionRejectedException(
            "Staff approval is required for AI tool: " + invocation.toolKey());
      }
      String missingArgument =
          definition.requiredArgumentNames().stream()
              .sorted()
              .filter(argument -> !invocation.arguments().containsKey(argument))
              .findFirst()
              .orElse(null);
      if (missingArgument != null) {
        throw new AiToolExecutionRejectedException(
            "Missing required AI tool argument: " + missingArgument);
      }
      String unknownArgument =
          definition.allowedArgumentNames().isEmpty()
              ? null
              : invocation.arguments().keySet().stream()
                  .sorted()
                  .filter(argument -> !definition.allowedArgumentNames().contains(argument))
                  .findFirst()
                  .orElse(null);
      if (unknownArgument != null) {
        throw new AiToolExecutionRejectedException("Unknown AI tool argument: " + unknownArgument);
      }
      String content =
          definition.handler().execute(toExecutionContext(context), invocation.arguments());
      record(
          new AiToolCallTrace(
              java.util.UUID.randomUUID(),
              invocation.toolKey(),
              definition.risk().name(),
              AiToolCallStatus.SUCCEEDED,
              true,
              invocation.userConfirmed(),
              invocation.staffApproved(),
              elapsedMillis(startedAt),
              invocation.arguments().toString(),
              content,
              null,
              null));
      return new AiToolResult(invocation.toolKey(), content, true);
    } catch (AiToolExecutionRejectedException rejected) {
      record(
          new AiToolCallTrace(
              java.util.UUID.randomUUID(),
              invocation.toolKey(),
              definition == null ? "UNKNOWN" : definition.risk().name(),
              AiToolCallStatus.REJECTED,
              authorized,
              invocation.userConfirmed(),
              invocation.staffApproved(),
              elapsedMillis(startedAt),
              invocation.arguments().toString(),
              null,
              rejected.getClass().getSimpleName(),
              rejected.getMessage()));
      throw rejected;
    } catch (RuntimeException failure) {
      record(
          new AiToolCallTrace(
              java.util.UUID.randomUUID(),
              invocation.toolKey(),
              definition == null ? "UNKNOWN" : definition.risk().name(),
              AiToolCallStatus.FAILED,
              authorized,
              invocation.userConfirmed(),
              invocation.staffApproved(),
              elapsedMillis(startedAt),
              invocation.arguments().toString(),
              null,
              failure.getClass().getSimpleName(),
              failure.getMessage()));
      throw failure;
    }
  }

  private void record(AiToolCallTrace trace) {
    try {
      traceRecorder.recordToolCall(trace);
    } catch (RuntimeException ignored) {
      // Trace persistence is best effort and must not alter tool semantics.
    }
  }

  private static long elapsedMillis(long startedAt) {
    return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
  }

  private static AiToolExecutionContext toExecutionContext(AiExecutionContext context) {
    return new AiToolExecutionContext(
        context.tenantId(),
        context.principalId(),
        context.roles(),
        context.conversationId(),
        context.workflowId(),
        context.traceId(),
        context.idempotencyKey());
  }
}
