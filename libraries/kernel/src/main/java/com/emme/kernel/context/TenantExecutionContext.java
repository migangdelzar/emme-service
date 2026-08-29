package com.emme.kernel.context;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable backend-resolved tenant context for one request or asynchronous execution.
 *
 * <p>The tenant ID is resolved by authenticated backend infrastructure. It must not be copied from
 * an LLM argument or accepted as an unverified client value. The database ID is optional because a
 * deployment may use the default database pool for a tenant.
 */
public record TenantExecutionContext(UUID tenantId, UUID databaseId, String correlationId) {

  public TenantExecutionContext {
    Objects.requireNonNull(tenantId, "tenantId must not be null");
    if (correlationId == null || correlationId.isBlank()) {
      throw new IllegalArgumentException("correlationId must not be blank");
    }
  }
}
