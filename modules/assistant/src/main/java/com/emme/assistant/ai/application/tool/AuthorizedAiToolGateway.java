package com.emme.assistant.ai.application.tool;

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

  public AuthorizedAiToolGateway(Collection<AiToolDefinition> definitions) {
    Objects.requireNonNull(definitions, "definitions must not be null");
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
  public AiToolResult execute(AiToolInvocation invocation) {
    Objects.requireNonNull(invocation, "invocation must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    AiToolDefinition definition = definitions.get(invocation.toolKey());
    if (definition == null) {
      throw new AiToolExecutionRejectedException("Unknown AI tool: " + invocation.toolKey());
    }
    if (!definition.isAuthorized(context.roles())) {
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
    String content =
        definition.handler().execute(toExecutionContext(context), invocation.arguments());
    return new AiToolResult(invocation.toolKey(), content, true);
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
