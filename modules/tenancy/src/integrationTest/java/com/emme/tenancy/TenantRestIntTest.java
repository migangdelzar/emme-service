package com.emme.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.TestApplication;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.tenancy.adapter.out.client.database.TenantSchemaName;
import com.emme.tenancy.adapter.out.client.database.TenantScopedDataSource;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import com.emme.tenancy.domain.model.TenantProvisioningState;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

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

  @Autowired private PostgreSQLContainer<?> postgresContainer;

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
  @DisplayName("Calendar event links reject duplicate appointment-provider rows")
  void calendarEventLinkCardinalityIsEnforcedByTheTenantMigration() throws Exception {
    UUID tenantId = UUID.randomUUID();
    String slug = "calendar-cardinality-" + UUID.randomUUID().toString().replace('-', 'a');
    String expectedSchema = TenantSchemaName.fromSlug(slug);
    UUID appointmentId = UUID.randomUUID();
    UUID firstEventId = UUID.randomUUID();
    UUID secondEventId = UUID.randomUUID();

    bootstrapJdbcClient.sql("CREATE EXTENSION IF NOT EXISTS vector SCHEMA emme_core").update();
    provisioningRepository.requestProvisioning(tenantId, slug, expectedSchema);
    assertThat(schemaMigrationPort.migrate(tenantId, slug)).isEqualTo(expectedSchema);

    TenantContextHolder.withTenantOverride(
        tenantId,
        () -> {
          try (Connection connection = tenantScopedDataSource.getConnection()) {
            insertCalendarEventLink(connection, appointmentId, firstEventId);
            try {
              insertCalendarEventLink(connection, appointmentId, secondEventId);
              throw new AssertionError("Duplicate calendar event link was accepted");
            } catch (java.sql.SQLException duplicate) {
              assertThat(duplicate.getSQLState()).isEqualTo("23505");
            }
          } catch (java.sql.SQLException failure) {
            throw new IllegalStateException(
                "Calendar event-link cardinality check failed", failure);
          }
        });
  }

  @Test
  @DisplayName("Tenant migrations enable RLS policies on tenant data tables")
  void tenantDataTablesHaveRowLevelSecurityPolicies() throws Exception {
    UUID tenantId = UUID.randomUUID();
    String slug = "rls-catalog-" + UUID.randomUUID().toString().replace('-', 'a');
    String expectedSchema = TenantSchemaName.fromSlug(slug);

    bootstrapJdbcClient.sql("CREATE EXTENSION IF NOT EXISTS vector SCHEMA emme_core").update();
    provisioningRepository.requestProvisioning(tenantId, slug, expectedSchema);
    assertThat(schemaMigrationPort.migrate(tenantId, slug)).isEqualTo(expectedSchema);

    try (Connection connection = dataSource.getConnection()) {
      for (String table : List.of("appointment", "calendar_event_link", "ai_semantic_cache")) {
        assertThat(hasRls(connection, expectedSchema, table))
            .as("RLS is enabled for %s.%s", expectedSchema, table)
            .isTrue();
        assertThat(hasTenantPolicy(connection, expectedSchema, table))
            .as("tenant policy exists for %s.%s", expectedSchema, table)
            .isTrue();
        assertThat(hasForcedRls(connection, expectedSchema, table))
            .as("RLS is forced for %s.%s", expectedSchema, table)
            .isTrue();
      }
    }
  }

  @Test
  @DisplayName("Tenant RLS rejects a mismatched tenant context inside the routed schema")
  void tenantRlsRejectsMismatchedTenantContextInsideRoutedSchema() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID otherTenantId = UUID.randomUUID();
    String slug = "rls-behavior-" + UUID.randomUUID().toString().replace('-', 'a');
    String expectedSchema = TenantSchemaName.fromSlug(slug);

    bootstrapJdbcClient.sql("CREATE EXTENSION IF NOT EXISTS vector SCHEMA emme_core").update();
    provisioningRepository.requestProvisioning(tenantId, slug, expectedSchema);
    assertThat(schemaMigrationPort.migrate(tenantId, slug)).isEqualTo(expectedSchema);

    String runtimeRole = "tenant_rls_runtime";
    String runtimePassword = "tenant_rls_runtime";
    bootstrapJdbcClient
        .sql(
            "DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '"
                + runtimeRole
                + "') THEN CREATE ROLE "
                + runtimeRole
                + " LOGIN PASSWORD '"
                + runtimePassword
                + "' NOSUPERUSER NOCREATEDB NOCREATEROLE; END IF; END $$")
        .update();
    bootstrapJdbcClient
        .sql("GRANT USAGE ON SCHEMA \"" + expectedSchema + "\" TO " + runtimeRole)
        .update();
    bootstrapJdbcClient
        .sql(
            "GRANT SELECT, INSERT ON TABLE \""
                + expectedSchema
                + "\".customer, \""
                + expectedSchema
                + "\".payment_webhook_event TO "
                + runtimeRole)
        .update();
    DataSource runtimeDataSource =
        new TenantScopedDataSource(
            new DriverManagerDataSource(
                postgresContainer.getJdbcUrl(), runtimeRole, runtimePassword),
            ignored -> expectedSchema);

    TenantContextHolder.withTenantOverride(
        tenantId,
        () -> {
          try (Connection connection = runtimeDataSource.getConnection()) {
            insertCustomer(connection, tenantId);
            insertWebhookEvent(connection, tenantId, "stripe", "event-a");
            setCurrentTenant(connection, otherTenantId);
            assertThat(countCustomers(connection)).isZero();
            assertThat(countWebhookEvents(connection)).isZero();
            setCurrentTenant(connection, tenantId);
            try {
              insertCustomer(connection, otherTenantId);
              throw new AssertionError("Mismatched tenant row was accepted");
            } catch (java.sql.SQLException failure) {
              assertThat(failure.getSQLState()).isEqualTo("42501");
            }
            try {
              insertWebhookEvent(connection, otherTenantId, "stripe", "event-b");
              throw new AssertionError("Mismatched webhook row was accepted");
            } catch (java.sql.SQLException failure) {
              assertThat(failure.getSQLState()).isEqualTo("42501");
            }
            assertThat(countCustomers(connection)).isEqualTo(1);
            assertThat(countWebhookEvents(connection)).isEqualTo(1);
          } catch (java.sql.SQLException failure) {
            throw new IllegalStateException("Tenant RLS behavior check failed", failure);
          }
        });
  }

  private static boolean hasRls(Connection connection, String schema, String table)
      throws java.sql.SQLException {
    try (var statement =
        connection.prepareStatement(
            "SELECT relrowsecurity FROM pg_class c "
                + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                + "WHERE n.nspname = ? AND c.relname = ?")) {
      statement.setString(1, schema);
      statement.setString(2, table);
      try (var result = statement.executeQuery()) {
        return result.next() && result.getBoolean(1);
      }
    }
  }

  private static boolean hasForcedRls(Connection connection, String schema, String table)
      throws java.sql.SQLException {
    try (var statement =
        connection.prepareStatement(
            "SELECT relforcerowsecurity FROM pg_class c "
                + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                + "WHERE n.nspname = ? AND c.relname = ?")) {
      statement.setString(1, schema);
      statement.setString(2, table);
      try (var result = statement.executeQuery()) {
        return result.next() && result.getBoolean(1);
      }
    }
  }

  private static boolean hasTenantPolicy(Connection connection, String schema, String table)
      throws java.sql.SQLException {
    try (var statement =
        connection.prepareStatement(
            "SELECT EXISTS (SELECT 1 FROM pg_policies "
                + "WHERE schemaname = ? AND tablename = ? AND policyname = 'tenant_isolation')")) {
      statement.setString(1, schema);
      statement.setString(2, table);
      try (var result = statement.executeQuery()) {
        return result.next() && result.getBoolean(1);
      }
    }
  }

  private static void insertCalendarEventLink(
      Connection connection, UUID appointmentId, UUID externalEventId)
      throws java.sql.SQLException {
    try (var statement =
        connection.prepareStatement(
            "INSERT INTO calendar_event_link "
                + "(tenant_id, appointment_id, provider, external_event_id) "
                + "VALUES (current_setting('app.current_tenant_id')::uuid, ?, 'GOOGLE_CALENDAR', ?)")) {
      statement.setObject(1, appointmentId);
      statement.setString(2, externalEventId.toString());
      statement.executeUpdate();
    }
  }

  private static void insertWebhookEvent(
      Connection connection, UUID tenantId, String provider, String eventId)
      throws java.sql.SQLException {
    try (var statement =
        connection.prepareStatement(
            "INSERT INTO payment_webhook_event "
                + "(tenant_id, provider, event_id) VALUES (?, ?, ?)")) {
      statement.setObject(1, tenantId);
      statement.setString(2, provider);
      statement.setString(3, eventId);
      statement.executeUpdate();
    }
  }

  private static int countWebhookEvents(Connection connection) throws java.sql.SQLException {
    try (var statement =
        connection.prepareStatement("SELECT COUNT(*) FROM payment_webhook_event")) {
      try (var result = statement.executeQuery()) {
        if (!result.next()) {
          throw new java.sql.SQLException("Webhook event count query returned no row");
        }
        return result.getInt(1);
      }
    }
  }

  private static void insertCustomer(Connection connection, UUID tenantId)
      throws java.sql.SQLException {
    try (var statement =
        connection.prepareStatement("INSERT INTO customer (tenant_id, name) VALUES (?, ?)")) {
      statement.setObject(1, tenantId);
      statement.setString(2, "RLS customer");
      statement.executeUpdate();
    }
  }

  private static void setCurrentTenant(Connection connection, UUID tenantId)
      throws java.sql.SQLException {
    try (var statement = connection.prepareStatement("SELECT set_config(?, ?, false)")) {
      statement.setString(1, "app.current_tenant_id");
      statement.setString(2, tenantId.toString());
      statement.executeQuery();
    }
  }

  private static int countCustomers(Connection connection) throws java.sql.SQLException {
    try (var statement = connection.prepareStatement("SELECT COUNT(*) FROM customer");
        var result = statement.executeQuery()) {
      assertThat(result.next()).isTrue();
      return result.getInt(1);
    }
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
  @DisplayName("Concurrent tenant activation claims publish only one activation opportunity")
  void concurrentActivationClaimsHaveOneWinner() throws Exception {
    UUID tenantId = UUID.randomUUID();
    String slug = "activation-race-" + UUID.randomUUID().toString().replace('-', 'a');
    String schemaName = TenantSchemaName.fromSlug(slug);
    provisioningRepository.requestProvisioning(tenantId, slug, schemaName);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      Future<Boolean> first = executor.submit(() -> claimActivation(tenantId, start));
      Future<Boolean> second = executor.submit(() -> claimActivation(tenantId, start));
      start.countDown();

      assertThat(first.get(10, TimeUnit.SECONDS) ^ second.get(10, TimeUnit.SECONDS)).isTrue();
    } finally {
      executor.shutdownNow();
    }

    assertThat(provisioningRepository.findStatus(tenantId).status())
        .isEqualTo(TenantProvisioningState.ACTIVE);
  }

  private boolean claimActivation(UUID tenantId, CountDownLatch start) {
    try {
      if (!start.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timed out waiting for activation claim race");
      }
      return provisioningRepository.claimActivation(tenantId);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted during activation claim race", interrupted);
    }
  }

  @Test
  @DisplayName("Invalid tenant schema migration fails before database work")
  void invalidTenantSchemaMigrationFailsBeforeDatabaseWork() {
    assertThatThrownBy(() -> schemaMigrationPort.migrate(UUID.randomUUID(), ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid tenant schema name");
  }
}
