package com.emme.assistant.ai.application.tool;

import com.emme.assistant.ai.application.port.out.AiToolIdempotencyStore;
import com.emme.assistant.ai.application.trace.AiToolCallStatus;
import com.emme.assistant.ai.application.trace.AiToolCallTrace;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.assistant.ai.application.trace.NoopAiTraceRecorder;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** In-process tool gateway that keeps authorization and tenant context outside model control. */
public final class AuthorizedAiToolGateway implements AiToolGateway {

  private final Map<String, AiToolDefinition> definitions;
  private final AiTraceRecorder traceRecorder;
  private final AiToolIdempotencyStore idempotencyStore;

  public AuthorizedAiToolGateway(Collection<AiToolDefinition> definitions) {
    this(definitions, NoopAiTraceRecorder.INSTANCE, NoopAiToolIdempotencyStore.INSTANCE);
  }

  public AuthorizedAiToolGateway(
      Collection<AiToolDefinition> definitions, AiTraceRecorder traceRecorder) {
    this(definitions, traceRecorder, NoopAiToolIdempotencyStore.INSTANCE);
  }

  public AuthorizedAiToolGateway(
      Collection<AiToolDefinition> definitions,
      AiTraceRecorder traceRecorder,
      AiToolIdempotencyStore idempotencyStore) {
    Objects.requireNonNull(definitions, "definitions must not be null");
    this.traceRecorder = Objects.requireNonNull(traceRecorder, "traceRecorder must not be null");
    this.idempotencyStore =
        Objects.requireNonNull(idempotencyStore, "idempotencyStore must not be null");
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
        .filter(definition -> definition.isAuthorized(context))
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
        .filter(definition -> definition.isAuthorized(context))
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public AiToolResult execute(AiToolInvocation invocation) {
    Objects.requireNonNull(invocation, "invocation must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    AiToolDefinition definition = definitions.get(invocation.toolKey());
    long startedAt = System.nanoTime();
    boolean authorized = definition != null && definition.isAuthorized(context);
    String claimedOperationKey = null;
    boolean claimed = false;
    boolean mutationHandlerExecuted = false;
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
      claimedOperationKey = mutationOperationKey(definition, context, invocation.arguments());
      if (claimedOperationKey != null) {
        Optional<AiToolResult> completed = idempotencyStore.find(claimedOperationKey);
        if (completed.isPresent()) {
          return completed.get();
        }
        if (!idempotencyStore.claim(claimedOperationKey, definition.key())) {
          completed = idempotencyStore.find(claimedOperationKey);
          if (completed.isPresent()) {
            return completed.get();
          }
          throw new AiToolExecutionRejectedException(
              "AI tool mutation is already in progress: " + definition.key());
        }
        claimed = true;
      }
      String content =
          definition.handler().execute(toExecutionContext(context), invocation.arguments());
      mutationHandlerExecuted = true;
      AiToolResult result = new AiToolResult(invocation.toolKey(), content, true);
      if (claimed) {
        idempotencyStore.complete(claimedOperationKey, result);
      }
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
      return result;
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
      if (claimed && !mutationHandlerExecuted) {
        try {
          idempotencyStore.release(claimedOperationKey);
        } catch (RuntimeException cleanupFailure) {
          failure.addSuppressed(cleanupFailure);
        }
      }
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

  private static String mutationOperationKey(
      AiToolDefinition definition, AiExecutionContext context, Map<String, String> arguments) {
    if (definition.risk() != AiToolRisk.MUTATION) {
      return null;
    }
    String canonical =
        arguments.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> frame(entry.getKey()) + frame(entry.getValue()))
            .collect(Collectors.joining("&"));
    return context.tenantId()
        + ":"
        + definition.key()
        + ":"
        + context.principalId()
        + ":"
        + context.idempotencyKey()
        + ":"
        + sha256(canonical);
  }

  private static String frame(String value) {
    return value.length() + ":" + value;
  }

  private static String sha256(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is unavailable", e);
    }
  }

  private static AiToolExecutionContext toExecutionContext(AiExecutionContext context) {
    return new AiToolExecutionContext(
        context.tenantId(),
        context.principalId(),
        context.roles(),
        context.conversationId(),
        context.workflowId(),
        context.traceId(),
        context.idempotencyKey(),
        context.channel(),
        context.tenantCapabilities(),
        context.enabledFeatures());
  }
}
