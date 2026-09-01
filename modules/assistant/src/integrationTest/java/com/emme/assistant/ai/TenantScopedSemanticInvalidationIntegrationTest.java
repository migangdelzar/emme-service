package com.emme.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.semantic.SemanticCacheDependencyChanged;
import com.emme.assistant.ai.adapter.out.persistence.JdbcAiTraceRecorder;
import com.emme.assistant.ai.adapter.out.persistence.JdbcSemanticCacheAdapter;
import com.emme.assistant.ai.application.semantic.SemanticCacheInvalidationService;
import com.emme.assistant.ai.application.trace.AiTraceRedactor;
import com.emme.assistant.ai.configuration.AiProperties;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.tenancy.adapter.out.client.database.TenantScopedDataSource;
import com.emme.tenancy.application.port.out.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class TenantScopedSemanticInvalidationIntegrationTest {

  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("emme_test")
          .withUsername("emme")
          .withPassword("emme");

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID OTHER_TENANT_ID = UUID.randomUUID();
  private static final UUID DATABASE_ID = UUID.randomUUID();

  @Container private static final PostgreSQLContainer<?> CONTAINER = POSTGRES;

  private JdbcTemplate adminJdbc;
  private JdbcTemplate scopedJdbc;
  private DataSource scopedDataSource;
  private SemanticCacheInvalidationService invalidation;

  @BeforeEach
  void setUpTenantScopedSemanticTables() {
    DataSource adminDataSource =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    adminJdbc = new JdbcTemplate(adminDataSource);
    scopedDataSource =
        new TenantScopedDataSource(adminDataSource, (Function<UUID, String>) ignored -> "public");
    scopedJdbc = new JdbcTemplate(scopedDataSource);

    adminJdbc.execute(
        """
        CREATE OR REPLACE FUNCTION current_tenant_id()
        RETURNS UUID
        LANGUAGE sql
        STABLE
        AS 'SELECT nullif(current_setting(''app.current_tenant_id'', true), '''')::UUID'
        """);
    adminJdbc.execute("DROP TABLE IF EXISTS ai_semantic_execution, ai_semantic_cache");
    adminJdbc.execute(
        """
        CREATE TABLE ai_semantic_cache (
            tenant_id UUID NOT NULL,
            principal_id UUID NOT NULL,
            cache_kind VARCHAR(40) NOT NULL,
            active BOOLEAN NOT NULL DEFAULT true,
            updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            version BIGINT NOT NULL DEFAULT 0
        )
        """);
    adminJdbc.execute(
        """
        CREATE TABLE ai_semantic_execution (
            id UUID NOT NULL,
            tenant_id UUID NOT NULL,
            principal_id UUID NOT NULL,
            conversation_id UUID,
            workflow_id UUID,
            trace_id VARCHAR(128),
            operation VARCHAR(80) NOT NULL,
            outcome VARCHAR(80) NOT NULL,
            top1_similarity DOUBLE PRECISION NOT NULL,
            top2_similarity DOUBLE PRECISION NOT NULL,
            margin DOUBLE PRECISION NOT NULL,
            matches JSONB NOT NULL,
            dependency VARCHAR(80),
            dependency_version VARCHAR(160),
            invalidation_context VARCHAR(2000),
            latency_ms BIGINT NOT NULL,
            PRIMARY KEY (tenant_id, id)
        )
        """);
    adminJdbc.update(
        "INSERT INTO ai_semantic_cache (tenant_id, principal_id, cache_kind) VALUES (?, ?, ?), (?, ?, ?)",
        TENANT_ID,
        UUID.randomUUID(),
        "CHAT_INFORMATIONAL",
        OTHER_TENANT_ID,
        UUID.randomUUID(),
        "CHAT_INFORMATIONAL");
    enableTenantRls("ai_semantic_cache");
    enableTenantRls("ai_semantic_execution");
    assertThat(isForcedRls("ai_semantic_execution")).isTrue();

    AiProperties properties =
        new AiProperties(
            "mock",
            null,
            new AiProperties.EmbeddingConfig("embeddinggemma:300m", "http://localhost", null, 2),
            true);
    TenantRepository tenants = mock(TenantRepository.class);
    when(tenants.findDatabaseIdByTenantId(TENANT_ID)).thenReturn(Optional.of(DATABASE_ID));
    invalidation =
        new SemanticCacheInvalidationService(
            new JdbcSemanticCacheAdapter(JdbcClient.create(scopedDataSource), properties),
            Optional.empty(),
            com.emme.assistant.ai.application.port.out.NoopSemanticMetrics.INSTANCE,
            new JdbcAiTraceRecorder(
                JdbcClient.create(scopedDataSource), new AiTraceRedactor(), new ObjectMapper()),
            tenants);
  }

  @AfterEach
  void clearTenant() {
    TenantContextHolder.withTenantOverride(
        TENANT_ID,
        () -> {
          return null;
        });
  }

  @Test
  void invalidationAndTraceRowsAreVisibleAndAffectedOnlyForTheEventTenant() {
    SemanticCacheDependencyChanged event =
        new SemanticCacheDependencyChanged(
            UUID.randomUUID(),
            TENANT_ID,
            null,
            SemanticCacheDependencyChanged.Dependency.PRICE,
            "price-v2",
            Instant.parse("2026-08-31T12:00:00Z"));

    assertThat(
            TenantContextHolder.withTenantOverride(
                TENANT_ID,
                () ->
                    scopedJdbc.queryForObject(
                        "SELECT current_setting('app.current_tenant_id')", String.class)))
        .isEqualTo(TENANT_ID.toString());
    invalidation.invalidate(event);

    assertThat(
            adminJdbc.queryForObject(
                "SELECT COUNT(*) FROM ai_semantic_cache WHERE tenant_id = ? AND NOT active",
                Integer.class,
                TENANT_ID))
        .isEqualTo(1);
    assertThat(
            adminJdbc.queryForObject(
                "SELECT COUNT(*) FROM ai_semantic_cache WHERE tenant_id = ? AND active",
                Integer.class,
                OTHER_TENANT_ID))
        .isEqualTo(1);
    assertThat(
            adminJdbc.queryForObject(
                "SELECT COUNT(*) FROM ai_semantic_execution WHERE tenant_id = ?",
                Integer.class,
                TENANT_ID))
        .isEqualTo(1);
    assertThat(
            adminJdbc.queryForObject(
                "SELECT COUNT(*) FROM ai_semantic_execution WHERE tenant_id = ?",
                Integer.class,
                OTHER_TENANT_ID))
        .isZero();
  }

  private void enableTenantRls(String table) {
    adminJdbc.execute("ALTER TABLE " + table + " ENABLE ROW LEVEL SECURITY");
    adminJdbc.execute("ALTER TABLE " + table + " FORCE ROW LEVEL SECURITY");
    adminJdbc.execute(
        "CREATE POLICY "
            + table
            + "_tenant_isolation ON "
            + table
            + " FOR ALL USING (tenant_id = current_tenant_id()) WITH CHECK (tenant_id = current_tenant_id())");
  }

  private boolean isForcedRls(String table) {
    return adminJdbc.queryForObject(
        "SELECT relforcerowsecurity FROM pg_class WHERE oid = ?::regclass", Boolean.class, table);
  }
}
