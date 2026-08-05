package com.emme.e2eprovisioner.tenant;

import java.sql.SQLException;
import java.util.UUID;

/** Port for provisioning the database records required by a tenant-owner E2E run. */
public interface TenantSeeder {

  UUID ensureTenant(String slug, String name) throws SQLException;

  void activateOwnerMembership(UUID tenantId, String userReference) throws SQLException;

  void cleanTenantData(UUID tenantId) throws SQLException;
}
