package com.emme.tenancy.application.service;

import com.emme.tenancy.api.usecase.EnsureTenantMembershipUseCase;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
class EnsureTenantMembershipService implements EnsureTenantMembershipUseCase {

  private final JdbcTemplate jdbc;

  EnsureTenantMembershipService(
      @Qualifier("bootstrapJdbcTemplate") JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void ensure(UUID tenantId, String userReference, String roleCode) {
    jdbc.update(
        "INSERT INTO emme_core.role (id, code, name, scope, active) "
            + "VALUES (gen_random_uuid(), ?, ?, 'TENANT', true) "
            + "ON CONFLICT (code) DO UPDATE SET active = true",
        roleCode, roleCode.replace("_", " "));
    jdbc.update(
        "INSERT INTO emme_core.membership (tenant_id, role_id, user_reference, status) "
            + "SELECT ?, r.id, ?, 'ACTIVE' FROM emme_core.role r WHERE r.code = ? "
            + "ON CONFLICT DO NOTHING",
        tenantId, userReference, roleCode);
  }
}
