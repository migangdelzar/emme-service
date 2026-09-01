package com.emme.ai.contracts.semantic;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Durable application event describing a tenant dependency that invalidates semantic responses. */
public record SemanticCacheDependencyChanged(
    UUID eventId,
    UUID tenantId,
    UUID principalId,
    Dependency dependency,
    String version,
    Instant occurredAt) {

  public SemanticCacheDependencyChanged {
    Objects.requireNonNull(eventId, "eventId must not be null");
    Objects.requireNonNull(tenantId, "tenantId must not be null");
    Objects.requireNonNull(dependency, "dependency must not be null");
    requireText(version, "version");
    Objects.requireNonNull(occurredAt, "occurredAt must not be null");
  }

  public enum Dependency {
    TENANT_POLICY,
    SERVICE,
    PRICE,
    QUOTE_TEMPLATE,
    MANUAL
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
  }
}
