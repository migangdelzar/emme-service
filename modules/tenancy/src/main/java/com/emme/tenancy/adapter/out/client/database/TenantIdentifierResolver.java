package com.emme.tenancy.adapter.out.client.database;

import com.emme.kernel.context.TenantContext;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;

public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

  private static final Logger log = LoggerFactory.getLogger(TenantIdentifierResolver.class);
  private static final String CORE_SCHEMA = "emme_core";

  private final Map<UUID, String> schemaCache = new ConcurrentHashMap<>();

  private JdbcClient bootstrapJdbc() {
    return ApplicationContextProvider.get().getBean("bootstrapJdbcClient", JdbcClient.class);
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
          bootstrapJdbc()
              .sql(
                  "SELECT schema_name FROM emme_core.tenant_registry "
                      + "WHERE tenant_id = CAST(:tenantId AS uuid)")
              .param("tenantId", tenantId)
              .query(String.class)
              .single();
      if (schemaName != null) {
        return TenantSchemaName.requireValid(schemaName);
      }
    } catch (Exception e) {
      log.warn("Failed to resolve schema for tenant {}: {}", tenantId, e.getMessage());
      throw new IllegalStateException("Unable to resolve schema for tenant: " + tenantId, e);
    }
    throw new IllegalStateException("Unable to resolve schema for tenant: " + tenantId);
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return true;
  }
}
