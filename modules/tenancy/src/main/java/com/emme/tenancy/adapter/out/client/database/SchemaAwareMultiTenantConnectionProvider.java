package com.emme.tenancy.adapter.out.client.database;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(name = "coreDataSource")
public class SchemaAwareMultiTenantConnectionProvider
    implements MultiTenantConnectionProvider<String> {

  private static final Logger log = LoggerFactory.getLogger(SchemaAwareMultiTenantConnectionProvider.class);
  private static final String CORE_SCHEMA = "emme_core";

  private final DataSource coreDataSource;
  private final TenantDatabasePoolProvider tenantPoolProvider;

  public SchemaAwareMultiTenantConnectionProvider(
      @Qualifier("coreDataSource") DataSource coreDataSource,
      TenantDatabasePoolProvider tenantPoolProvider) {
    this.coreDataSource = coreDataSource;
    this.tenantPoolProvider = tenantPoolProvider;
  }

  @Override
  public Connection getConnection(String tenantIdentifier) throws SQLException {
    if (CORE_SCHEMA.equals(tenantIdentifier)) {
      return coreDataSource.getConnection();
    }
    Connection connection = tenantPoolProvider.getDataSource().getConnection();
    connection.setSchema(tenantIdentifier);
    log.debug("Connection routed to schema {}", tenantIdentifier);
    return connection;
  }

  @Override
  public void releaseConnection(String tenantIdentifier, Connection connection)
      throws SQLException {
    connection.setSchema(CORE_SCHEMA);
    connection.close();
  }

  @Override
  public Connection getAnyConnection() throws SQLException {
    return coreDataSource.getConnection();
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
