package com.emme.tenancy.adapter.out.persistence.adapter;

import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Implements tenant provisioning registry lifecycle operations with JDBC. */
@Component
public final class JdbcTenantProvisioningRepository implements TenantProvisioningRepository {

  private final JdbcTemplate jdbc;

  public JdbcTenantProvisioningRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<TenantProvisioningRequest> findPending() {
    return jdbc.query(
        "SELECT tenant_id, slug, schema_name FROM emme_core.tenant_registry WHERE status = 'PROVISIONING'",
        (rs, rowNum) ->
            new TenantProvisioningRequest(
                rs.getObject("tenant_id", UUID.class),
                rs.getString("slug"),
                rs.getString("schema_name")));
  }

  @Override
  public void markActive(UUID tenantId) {
    jdbc.update(
        "UPDATE emme_core.tenant_registry SET status = 'ACTIVE', schema_version = '0.1.0', last_migrated_at = now(), migration_error = NULL, updated_at = now() WHERE tenant_id = ?",
        tenantId);
  }

  @Override
  public void markFailed(UUID tenantId, String error) {
    jdbc.update(
        "UPDATE emme_core.tenant_registry SET status = 'FAILED', migration_error = ?, updated_at = now() WHERE tenant_id = ?",
        error,
        tenantId);
  }
}
