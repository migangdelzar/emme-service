package com.emme.assistant.ai.application.semantic;

import com.emme.ai.contracts.semantic.SemanticCacheDependencyChanged.Dependency;
import java.util.Objects;
import java.util.UUID;

/** Tenant/principal target for invalidating one semantic-cache kind. */
public record SemanticCacheInvalidation(
    UUID tenantId, UUID principalId, String cacheKind, Dependency dependency, String version) {

  public SemanticCacheInvalidation {
    Objects.requireNonNull(tenantId, "tenantId must not be null");
    if (cacheKind == null || cacheKind.isBlank()) {
      throw new IllegalArgumentException("cacheKind must not be blank");
    }
    Objects.requireNonNull(dependency, "dependency must not be null");
    if (version == null || version.isBlank()) {
      throw new IllegalArgumentException("version must not be blank");
    }
  }
}
