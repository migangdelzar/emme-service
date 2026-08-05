package com.emme.tenancy.adapter.out.client.database;

import com.emme.kernel.context.TenantContext;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(name = "bootstrapJdbcTemplate")
public class CurrentTenantIdentifierResolver
    implements org.hibernate.context.spi.CurrentTenantIdentifierResolver<String> {

  private static final Logger log = LoggerFactory.getLogger(CurrentTenantIdentifierResolver.class);
  private static final String CORE_SCHEMA = "emme_core";

  private final JdbcTemplate bootstrapJdbc;
  private final Map<UUID, String> schemaCache = new ConcurrentHashMap<>();

  public CurrentTenantIdentifierResolver(
      @Qualifier("bootstrapJdbcTemplate") JdbcTemplate bootstrapJdbc) {
    this.bootstrapJdbc = bootstrapJdbc;
  }

  @Override
  public String resolveCurrentTenantIdentifier() {
    UUID tenantId = TenantContext.getCurrentTenantId();
    if (tenantId == null) {
      return CORE_SCHEMA;
    }
    return schemaCache.computeIfAbsent(tenantId, this::lookupSchemaName);
  }

  private String lookupSchemaName(UUID tenantId) {
    try {
      String schemaName =
          bootstrapJdbc.queryForObject(
              "SELECT schema_name FROM emme_core.tenant_registry WHERE tenant_id = ?::uuid",
              String.class,
              tenantId.toString());
      if (schemaName != null) {
        String validated = TenantSchemaName.requireValid(schemaName);
        log.debug("Resolved tenant {} to schema {}", tenantId, validated);
        return validated;
      }
    } catch (Exception e) {
      log.warn("Failed to resolve schema for tenant {}: {}", tenantId, e.getMessage());
    }
    log.warn("Tenant {} not found in registry, falling back to {}", tenantId, CORE_SCHEMA);
    return CORE_SCHEMA;
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return true;
  }
}
