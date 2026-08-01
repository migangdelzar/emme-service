package com.emme.tenancy.adapter.out.client.database;

import com.emme.tenancy.application.port.out.DatabaseRegistryEntry;
import com.emme.tenancy.application.port.out.DatabaseRegistryPort;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.stereotype.Component;

/**
 * Bootstrap JDBC registry lookup — connects DIRECTLY to PostgreSQL, bypassing the tenant-aware
 * DataSource. This breaks the circular dependency between entityManagerFactory → DataSource →
 * TenantDatabasePoolProvider → DatabaseRegistryAdapter.
 *
 * <p>Uses raw JDBC with a dedicated bootstrap connection. This connection is only used for looking
 * up the {@code database_registry} table during pool initialization. All business queries use the
 * tenant-aware DataSource.
 */
@Component
public class DatabaseRegistryAdapter implements DatabaseRegistryPort {

  private static final UUID DEFAULT_DB_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private final String bootstrapUrl;
  private final String username;
  private final String password;

  public DatabaseRegistryAdapter(JdbcConnectionDetails connectionDetails) {
    this.bootstrapUrl = connectionDetails.getJdbcUrl();
    this.username = connectionDetails.getUsername();
    this.password = connectionDetails.getPassword();
  }

  @Override
  public Optional<DatabaseRegistryEntry> findById(UUID id) {
    // Default database — hardcoded, no DB query needed (avoids chicken-and-egg)
    if (DEFAULT_DB_ID.equals(id)) {
      return Optional.of(
          new DatabaseRegistryEntry(DEFAULT_DB_ID, "default", bootstrapUrl, 3, 20, 0, true));
    }

    // Tenant databases — look up from registry table
    try (Connection conn = DriverManager.getConnection(bootstrapUrl, username, password);
        var stmt =
            conn.prepareStatement(
                "SELECT database_id, jdbc_url, name, min_pool_size, max_pool_size, priority, is_active "
                    + "FROM emme_core.database_registry WHERE database_id = ?")) {
      stmt.setObject(1, id);
      try (var rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(
              new DatabaseRegistryEntry(
                  UUID.fromString(rs.getString("database_id")),
                  rs.getString("name"),
                  rs.getString("jdbc_url"),
                  rs.getInt("min_pool_size"),
                  rs.getInt("max_pool_size"),
                  rs.getInt("priority"),
                  rs.getBoolean("is_active")));
        }
        return Optional.empty();
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to lookup database registry for id=" + id, e);
    }
  }
}
