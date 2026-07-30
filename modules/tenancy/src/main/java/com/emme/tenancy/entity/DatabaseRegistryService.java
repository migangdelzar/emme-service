package com.emme.tenancy.entity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Bootstrap JDBC registry lookup — connects DIRECTLY to PostgreSQL, bypassing the tenant-aware
 * DataSource. This breaks the circular dependency between entityManagerFactory → DataSource →
 * DatabasePoolManager → DatabaseRegistryService.
 *
 * <p>Uses raw JDBC with a dedicated bootstrap connection. This connection is only used for looking
 * up the {@code database_registry} table during pool initialization. All business queries use the
 * tenant-aware DataSource.
 */
@Service
public class DatabaseRegistryService {

  private static final UUID DEFAULT_DB_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private final String bootstrapUrl;
  private final String username;
  private final String password;

  public DatabaseRegistryService(
      @Value("${spring.datasource.url}") String bootstrapUrl,
      @Value("${spring.datasource.username:emme}") String username,
      @Value("${spring.datasource.password:emme}") String password) {
    this.bootstrapUrl = bootstrapUrl;
    this.username = username;
    this.password = password;
  }

  public Optional<DatabaseRegistry> findById(UUID id) {
    // Default database — hardcoded, no DB query needed (avoids chicken-and-egg)
    if (DEFAULT_DB_ID.equals(id)) {
      var db = new DatabaseRegistry();
      db.setDatabaseId(DEFAULT_DB_ID);
      db.setJdbcUrl(bootstrapUrl);
      db.setName("default");
      db.setMinPoolSize(3);
      db.setMaxPoolSize(20);
      db.setPriority(0);
      db.setIsActive(true);
      return Optional.of(db);
    }

    // Tenant databases — look up from registry table
    try (Connection conn = DriverManager.getConnection(bootstrapUrl, username, password)) {
      var stmt =
          conn.prepareStatement(
              "SELECT database_id, jdbc_url, name, min_pool_size, max_pool_size, priority, is_active "
                  + "FROM emme_core.database_registry WHERE database_id = ?");
      stmt.setObject(1, id);
      var rs = stmt.executeQuery();
      if (rs.next()) {
        var db = new DatabaseRegistry();
        db.setDatabaseId(UUID.fromString(rs.getString("database_id")));
        db.setJdbcUrl(rs.getString("jdbc_url"));
        db.setName(rs.getString("name"));
        db.setMinPoolSize(rs.getInt("min_pool_size"));
        db.setMaxPoolSize(rs.getInt("max_pool_size"));
        db.setPriority(rs.getInt("priority"));
        db.setIsActive(rs.getBoolean("is_active"));
        return Optional.of(db);
      }
      return Optional.empty();
    } catch (Exception e) {
      throw new RuntimeException("Failed to lookup database registry for id=" + id, e);
    }
  }
}
