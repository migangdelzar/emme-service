package com.emme.ai.contracts.tenant;

import com.emme.kernel.context.TenantExecutionContext;
import java.util.UUID;

/** Resolves an authoritative tenant execution context for AI background work. */
@FunctionalInterface
public interface AiTenantContextResolver {

  /**
   * Resolves the tenant database and correlation context without exposing tenancy implementation
   * details to an AI consumer.
   *
   * @throws IllegalStateException when no safe database can be selected
   */
  TenantExecutionContext resolve(UUID tenantId, String correlationId);
}
