package com.emme.tenancy.adapter.out.client.database;

import com.emme.tenancy.adapter.out.client.database.TenantDatabasePoolProvider;
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

  private DataSource coreDataSource() {
    var ctx = ApplicationContextProvider.get();
    if (ctx != null) {
      return ctx.getBean("coreDataSource", DataSource.class);
    }
    throw new IllegalStateException("coreDataSource not available — context not initialized");
  }

  private TenantDatabasePoolProvider tenantPoolProvider() {
    var ctx = ApplicationContextProvider.get();
    if (ctx != null) {
      return ctx.getBean(TenantDatabasePoolProvider.class);
    }
    throw new IllegalStateException("tenantPoolProvider not available — context not initialized");
  }

  @Override
  public Connection getConnection(String tenantIdentifier) throws SQLException {
    if (CORE_SCHEMA.equals(tenantIdentifier)) {
      return coreDataSource().getConnection();
    }
    Connection connection = tenantPoolProvider().getDataSource().getConnection();
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
      return coreDataSource().getConnection();
    }
    return java.sql.DriverManager.getConnection(
        "jdbc:postgresql://localhost:5432/emme", "emme", "emme");
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
