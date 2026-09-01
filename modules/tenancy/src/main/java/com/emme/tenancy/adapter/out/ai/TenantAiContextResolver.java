package com.emme.tenancy.adapter.out.ai;

import com.emme.ai.contracts.tenant.AiTenantContextResolver;
import com.emme.kernel.context.TenantExecutionContext;
import com.emme.tenancy.application.port.out.TenantRepository;
import com.emme.tenancy.configuration.TenantPoolingProperties;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adapts authoritative tenancy data to the AI tenant-context contract. */
@Component
public final class TenantAiContextResolver implements AiTenantContextResolver {

  private final TenantRepository tenants;
  private final TenantPoolingProperties poolingProperties;

  public TenantAiContextResolver(
      TenantRepository tenants, TenantPoolingProperties poolingProperties) {
    this.tenants = Objects.requireNonNull(tenants, "tenants must not be null");
    this.poolingProperties =
        Objects.requireNonNull(poolingProperties, "poolingProperties must not be null");
  }

  @Override
  public TenantExecutionContext resolve(UUID tenantId, String correlationId) {
    Objects.requireNonNull(tenantId, "tenantId must not be null");
    Objects.requireNonNull(correlationId, "correlationId must not be null");
    UUID databaseId =
        tenants
            .findDatabaseIdByTenantId(tenantId)
            .or(this::configuredDefaultDatabaseId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No valid default database for semantic cache invalidation"));
    return new TenantExecutionContext(tenantId, databaseId, correlationId);
  }

  private Optional<UUID> configuredDefaultDatabaseId() {
    String configured = poolingProperties.defaultDatabaseId();
    if (configured == null || configured.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(configured));
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
  }
}
