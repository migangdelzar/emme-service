package com.emme.e2eprovisioner.tenant;

import com.emme.shared.persistence.jdbc.JdbcConnectionExecutor;
import com.emme.shared.persistence.jdbc.ThrowingSqlConnectionFunction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;

/** JDBC adapter that performs idempotent, prepared tenant-owner seed operations. */
public final class JdbcTenantSeeder implements TenantSeeder {

  private static final UUID BUSINESS_OWNER_ROLE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private final JdbcConnectionExecutor connectionExecutor;

  public JdbcTenantSeeder(JdbcConnectionExecutor connectionExecutor) {
    this.connectionExecutor = connectionExecutor;
  }

  public static JdbcTenantSeeder create(DataSource dataSource) {
    return new JdbcTenantSeeder(
        new JdbcConnectionExecutor(new org.springframework.jdbc.core.JdbcTemplate(dataSource)));
  }

  @Override
  public UUID ensureTenant(String slug, String name) throws SQLException {
    return connectionExecutor.withConnection(
        (ThrowingSqlConnectionFunction<UUID, SQLException>)
            connection ->
                inTransaction(connection, ignored -> ensureTenant(connection, slug, name)));
  }

  @Override
  public void activateOwnerMembership(UUID tenantId, String userReference) throws SQLException {
    connectionExecutor.consumeWithConnection(
        connection -> {
          inTransaction(
              connection,
              ignored -> {
                activateOwnerMembership(connection, tenantId, userReference);
                ensureSubscription(connection, tenantId);
                ensurePermissions(connection, tenantId);
                return null;
              });
        });
  }

  @Override
  public void cleanTenantData(UUID tenantId) throws SQLException {
    connectionExecutor.consumeWithConnection(
        connection -> {
          inTransaction(
              connection,
              ignored -> {
                cleanBusinessData(connection, tenantId);
                return null;
              });
        });
  }

  private static UUID ensureTenant(Connection connection, String slug, String name)
      throws SQLException {
    UUID tenantId;
    try (var statement =
        connection.prepareStatement(
            "INSERT INTO emme_core.tenant_registry (slug, schema_name, database_mode, status) "
                + "VALUES (?, ?, 'SHARED', 'PROVISIONING') "
                + "ON CONFLICT (slug) DO UPDATE SET status = 'PROVISIONING' "
                + "RETURNING tenant_id")) {
      var schemaName = slug.replaceAll("[^a-z0-9-]", "").replace("-", "_");
      statement.setString(1, slug);
      statement.setString(2, schemaName);
      try (var result = statement.executeQuery()) {
        if (!result.next()) throw new SQLException("Failed to create tenant: " + slug);
        tenantId = result.getObject(1, UUID.class);
      }
    }

    // Also create the tenant metadata record
    try (var statement =
        connection.prepareStatement(
            """
            INSERT INTO emme_core.tenant (id, slug, name, status, keycloak_realm)
            VALUES (?, ?, ?, 'ACTIVE', 'emme')
            ON CONFLICT (id) DO UPDATE SET
              slug = EXCLUDED.slug,
              name = EXCLUDED.name,
              status = 'ACTIVE'
            """)) {
      statement.setObject(1, tenantId);
      statement.setString(2, slug);
      statement.setString(3, name);
      statement.executeUpdate();
    }
    return tenantId;
  }

  private static void activateOwnerMembership(
      Connection connection, UUID tenantId, String userReference) throws SQLException {
    try (var statement =
        connection.prepareStatement(
            """
            INSERT INTO emme_core.role (id, code, name, scope, active)
            VALUES (?, 'business_owner', 'Business owner', 'TENANT', true)
            ON CONFLICT (code) DO UPDATE SET active = true
            """)) {
      statement.setObject(1, BUSINESS_OWNER_ROLE_ID);
      statement.executeUpdate();
    }

    try (var statement =
        connection.prepareStatement(
            """
            INSERT INTO emme_core.membership (tenant_id, role_id, user_reference, status)
            VALUES (?, ?, ?, 'ACTIVE')
            ON CONFLICT DO NOTHING
            """)) {
      statement.setObject(1, tenantId);
      statement.setObject(2, BUSINESS_OWNER_ROLE_ID);
      statement.setString(3, userReference);
      statement.executeUpdate();
    }

    try (var statement =
        connection.prepareStatement(
            """
            UPDATE emme_core.tenant_registry
            SET status = 'ACTIVE',
                schema_version = '0.1.0',
                last_migrated_at = now(),
                migration_error = NULL,
                updated_at = now()
            WHERE tenant_id = ?
            """)) {
      statement.setObject(1, tenantId);
      statement.executeUpdate();
    }
  }

  private static void cleanBusinessData(Connection connection, UUID tenantId)
      throws SQLException {
    try (var stmt = connection.prepareStatement(
        "DELETE FROM e2e_studio.appointment WHERE tenant_id = ?")) {
      stmt.setObject(1, tenantId); stmt.executeUpdate();
    }
    try (var stmt = connection.prepareStatement(
        "DELETE FROM e2e_studio.customer WHERE tenant_id = ?")) {
      stmt.setObject(1, tenantId); stmt.executeUpdate();
    }
    try (var stmt = connection.prepareStatement(
        "DELETE FROM e2e_studio.service WHERE tenant_id = ?")) {
      stmt.setObject(1, tenantId); stmt.executeUpdate();
    }
    try (var stmt = connection.prepareStatement(
        "DELETE FROM e2e_studio.artist_capability WHERE tenant_id = ?")) {
      stmt.setObject(1, tenantId); stmt.executeUpdate();
    }
    try (var stmt = connection.prepareStatement(
        "DELETE FROM e2e_studio.artist WHERE tenant_id = ?")) {
      stmt.setObject(1, tenantId); stmt.executeUpdate();
    }
  }

  private static void ensureSubscription(Connection connection, UUID tenantId)
      throws SQLException {
    try (var statement =
        connection.prepareStatement(
            """
            INSERT INTO e2e_studio.subscription (id, tenant_id, plan, status, period_ends_at, updated_at)
            VALUES (gen_random_uuid(), ?, 'PRO', 'ACTIVE', now() + interval '30 days', now())
            ON CONFLICT DO NOTHING
            """)) {
      statement.setObject(1, tenantId);
      statement.executeUpdate();
    }
  }

  private static void ensurePermissions(Connection connection, UUID tenantId)
      throws SQLException {
    // Seed read/write permissions for the business_owner role
    var permissions = new String[] {
      "appointment:read", "appointment:write", "appointment:cancel",
      "customer:read", "customer:write",
      "service:read", "service:write",
      "artist:read", "artist:write",
      "finances:read", "business:read", "business:write"
    };
    for (var perm : permissions) {
      var permId = UUID.nameUUIDFromBytes(perm.getBytes());
      try (var s = connection.prepareStatement(
          "INSERT INTO emme_core.permission (id, code, name, description, active) " +
          "VALUES (?, ?, ?, ?, true) ON CONFLICT (code) DO UPDATE SET active = true")) {
        s.setObject(1, permId);
        s.setString(2, perm);
        s.setString(3, perm);
        s.setString(4, "E2E auto-granted permission");
        s.executeUpdate();
      }
      try (var s = connection.prepareStatement(
          "INSERT INTO emme_core.role_permission (id, role_id, permission_id, granted_at) " +
          "VALUES (gen_random_uuid(), ?, ?, now()) ON CONFLICT DO NOTHING")) {
        s.setObject(1, BUSINESS_OWNER_ROLE_ID);
        s.setObject(2, permId);
        s.executeUpdate();
      }
    }
  }

  private static <T> T inTransaction(
      Connection connection, ThrowingSqlConnectionFunction<T, SQLException> operation)
      throws SQLException {
    var originalAutoCommit = connectionAutoCommit(connection);
    try {
      connection.setAutoCommit(false);
      var result = operation.apply(connection);
      connection.commit();
      return result;
    } catch (SQLException exception) {
      rollback(connection, exception);
      throw exception;
    } finally {
      connection.setAutoCommit(originalAutoCommit);
    }
  }

  private static boolean connectionAutoCommit(Connection connection) throws SQLException {
    return connection.getAutoCommit();
  }

  private static void rollback(Connection connection, SQLException original) {
    try {
      connection.rollback();
    } catch (SQLException rollbackFailure) {
      original.addSuppressed(rollbackFailure);
    }
  }
}
