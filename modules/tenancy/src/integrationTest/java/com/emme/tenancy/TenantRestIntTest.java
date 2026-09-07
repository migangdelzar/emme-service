package com.emme.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.TestApplication;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.tenancy.adapter.out.client.database.TenantSchemaName;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import com.emme.tenancy.domain.model.TenantProvisioningState;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import java.sql.Connection;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Pilot integration test proving:
 *
 * <ul>
 *   <li>PostgreSQL 16 container starts via {@code @ServiceConnection}
 *   <li>Spring context boots with {@link TestApplication}
 *   <li>JDBC connection works against the Testcontainers PostgreSQL
 * </ul>
 */
@SpringBootTest(classes = TestApplication.class)
@PostgresIntegrationTest
@DisplayName("Tenancy PostgreSQL integration test pilot")
class TenantRestIntTest {

  @Autowired private DataSource dataSource;

  @Autowired
  @Qualifier("tenantScopedDataSource")
  private DataSource tenantScopedDataSource;

  @Autowired private TenantProvisioningRepository provisioningRepository;

  @Autowired private TenantSchemaMigrationPort schemaMigrationPort;

  @Autowired
  @Qualifier("bootstrapJdbcClient")
  private JdbcClient bootstrapJdbcClient;

  @Test
  @DisplayName("PostgreSQL container is wired via @ServiceConnection")
  void postgresContainerIsWired() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      var stmt = conn.createStatement();
      var rs = stmt.executeQuery("SELECT 1");
      assertThat(rs.next()).isTrue();
      assertThat(rs.getInt(1)).isEqualTo(1);
    }
  }

  @Test
  @DisplayName("Spring context boots successfully")
  void contextLoads() {
    assertThat(dataSource).isNotNull();
  }

  @Test
  @DisplayName("Liquibase migration and checkout route tenant data to the tenant schema")
  void migratesAndRoutesTenantSchema() throws Exception {
    UUID tenantId = UUID.randomUUID();
    String slug = "live-routing-" + UUID.randomUUID().toString().replace('-', 'a');
    String expectedSchema = TenantSchemaName.fromSlug(slug);
    bootstrapJdbcClient.sql("CREATE EXTENSION IF NOT EXISTS vector SCHEMA emme_core").update();
    provisioningRepository.requestProvisioning(tenantId, slug, expectedSchema);

    assertThat(schemaMigrationPort.migrate(tenantId, slug)).isEqualTo(expectedSchema);

    TenantContextHolder.withTenantOverride(
        tenantId,
        () -> {
          try (Connection connection = tenantScopedDataSource.getConnection();
              var statement =
                  connection.prepareStatement(
                      "SELECT table_schema, table_name, current_schema(), "
                          + "current_setting('app.current_tenant_id') "
                          + "FROM information_schema.tables "
                          + "WHERE table_schema = ? AND table_name = 'tenant_schema_metadata'")) {
            statement.setString(1, expectedSchema);
            try (var resultSet = statement.executeQuery()) {
              assertThat(resultSet.next()).isTrue();
              assertThat(resultSet.getString(1)).isEqualTo(expectedSchema);
              assertThat(resultSet.getString(2)).isEqualTo("tenant_schema_metadata");
              assertThat(resultSet.getString(3)).isEqualTo(expectedSchema);
              assertThat(resultSet.getString(4)).isEqualTo(tenantId.toString());
            }
          }
        });
  }

  @Test
  @DisplayName("Duplicate tenant provisioning keeps the original registry owner")
  void duplicateProvisioningKeepsOriginalRegistryOwner() {
    UUID originalTenantId = UUID.randomUUID();
    UUID duplicateTenantId = UUID.randomUUID();
    String slug = "duplicate-" + UUID.randomUUID().toString().replace('-', 'a');
    String schemaName = TenantSchemaName.fromSlug(slug);

    assertThat(provisioningRepository.requestProvisioning(originalTenantId, slug, schemaName))
        .isEqualTo(originalTenantId);
    assertThat(provisioningRepository.requestProvisioning(duplicateTenantId, slug, schemaName))
        .isEqualTo(originalTenantId);

    assertThat(provisioningRepository.findStatus(originalTenantId).status())
        .isEqualTo(TenantProvisioningState.PROVISIONING);
    assertThatThrownBy(() -> provisioningRepository.findStatus(duplicateTenantId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Tenant registry not found: " + duplicateTenantId);
  }

  @Test
  @DisplayName("Invalid tenant schema migration fails before database work")
  void invalidTenantSchemaMigrationFailsBeforeDatabaseWork() {
    assertThatThrownBy(() -> schemaMigrationPort.migrate(UUID.randomUUID(), ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid tenant schema name");
  }
}
