package com.emme.assistant.ai.application.tool;

import com.emme.kernel.context.Channel;
import java.util.Set;
import java.util.UUID;

/** Backend-created context passed to a tool handler; no tenant or identity comes from the model. */
public record AiToolExecutionContext(
    UUID tenantId,
    UUID principalId,
    Set<String> roles,
    UUID conversationId,
    UUID workflowId,
    String traceId,
    String idempotencyKey,
    Channel channel,
    Set<String> tenantCapabilities,
    Set<String> enabledFeatures) {

  public AiToolExecutionContext(
      UUID tenantId,
      UUID principalId,
      Set<String> roles,
      UUID conversationId,
      UUID workflowId,
      String traceId,
      String idempotencyKey) {
    this(
        tenantId,
        principalId,
        roles,
        conversationId,
        workflowId,
        traceId,
        idempotencyKey,
        Channel.INTERNAL,
        Set.of(),
        Set.of());
  }

  public AiToolExecutionContext {
    if (tenantId == null) throw new NullPointerException("tenantId must not be null");
    if (principalId == null) throw new NullPointerException("principalId must not be null");
    roles = Set.copyOf(roles == null ? Set.of() : roles);
    if (conversationId == null) throw new NullPointerException("conversationId must not be null");
    if (workflowId == null) throw new NullPointerException("workflowId must not be null");
    requireText(traceId, "traceId");
    requireText(idempotencyKey, "idempotencyKey");
    if (channel == null) throw new NullPointerException("channel must not be null");
    tenantCapabilities = Set.copyOf(tenantCapabilities == null ? Set.of() : tenantCapabilities);
    enabledFeatures = Set.copyOf(enabledFeatures == null ? Set.of() : enabledFeatures);
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
