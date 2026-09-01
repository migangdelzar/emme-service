package com.emme.kernel.context;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable, backend-resolved identity and correlation context for an AI operation.
 *
 * <p>Tenant, principal, and role values must be resolved before this context is created. Model
 * output and client-provided values must never be used as a replacement for this context.
 */
public record AiExecutionContext(
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

  public AiExecutionContext(
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

  public AiExecutionContext {
    tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
    principalId = Objects.requireNonNull(principalId, "principalId must not be null");
    roles = Objects.requireNonNull(roles, "roles must not be null");
    conversationId = Objects.requireNonNull(conversationId, "conversationId must not be null");
    workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
    traceId = requireText(traceId, "traceId");
    idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
    channel = Objects.requireNonNull(channel, "channel must not be null");
    tenantCapabilities = immutableTextSet(tenantCapabilities, "tenantCapabilities");
    enabledFeatures = immutableTextSet(enabledFeatures, "enabledFeatures");

    if (roles.stream().anyMatch(role -> role == null || role.isBlank())) {
      throw new IllegalArgumentException("roles must not contain blank values");
    }
    roles = Set.copyOf(roles);
  }

  /** Returns a context with backend-resolved resource correlation identifiers. */
  public AiExecutionContext withWorkflow(UUID conversationId, UUID workflowId) {
    return new AiExecutionContext(
        tenantId,
        principalId,
        roles,
        conversationId,
        workflowId,
        traceId,
        idempotencyKey,
        channel,
        tenantCapabilities,
        enabledFeatures);
  }

  private static Set<String> immutableTextSet(Set<String> values, String field) {
    Objects.requireNonNull(values, field + " must not be null");
    if (values.stream().anyMatch(value -> value == null || value.isBlank())) {
      throw new IllegalArgumentException(field + " must not contain blank values");
    }
    return Set.copyOf(values);
  }

  private static String requireText(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value;
  }
}
