package com.emme.tenancy.adapter.out.client.database;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SchemaAwareMultiTenantConnectionProvider
    implements MultiTenantConnectionProvider<String> {

  private static final Logger log =
      LoggerFactory.getLogger(SchemaAwareMultiTenantConnectionProvider.class);
  private static final String CORE_SCHEMA = "emme_core";

  private DataSource core() {
    return ApplicationContextProvider.get().getBean(DataSource.class);
  }

  private TenantDatabasePoolProvider tenant() {
    return ApplicationContextProvider.get().getBean(TenantDatabasePoolProvider.class);
  }

  @Override
  public Connection getConnection(String tenantIdentifier) throws SQLException {
    if (CORE_SCHEMA.equals(tenantIdentifier)) {
      return core().getConnection();
    }
    Connection connection = tenant().getDataSource().getConnection();
    connection.setSchema(tenantIdentifier);
    log.debug("Connection routed to schema {}", tenantIdentifier);
    return connection;
  }

  @Override
  public void releaseConnection(String tenantIdentifier, Connection connection)
      throws SQLException {
    try {
      connection.setSchema(CORE_SCHEMA);
    } finally {
      connection.close();
    }
  }

  @Override
  public Connection getAnyConnection() throws SQLException {
    if (ApplicationContextProvider.get() != null) {
      return core().getConnection();
    }
    var host = System.getenv().getOrDefault("DB_HOST", "localhost");
    var port = System.getenv().getOrDefault("DB_PORT", "5432");
    var user = System.getenv().getOrDefault("DB_USERNAME", "emme");
    var pass = System.getenv().getOrDefault("DB_PASSWORD", "emme");
    return java.sql.DriverManager.getConnection(
        "jdbc:postgresql://" + host + ":" + port + "/emme", user, pass);
  }

  @Override
  public void releaseAnyConnection(Connection connection) throws SQLException {
    connection.close();
  }

  @Override
  public boolean supportsAggressiveRelease() {
    return false;
  }

  @Override
  public boolean isUnwrappableAs(Class<?> unwrapType) {
    return false;
  }

  @Override
  public <T> T unwrap(Class<T> unwrapType) {
    throw new UnknownUnwrapTypeException(unwrapType);
  }
}
