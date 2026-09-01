package com.emme.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.contracts.semantic.EmbeddingModelDefaults;
import com.emme.assistant.ai.adapter.out.persistence.JdbcAiToolIdempotencyStore;
import com.emme.assistant.ai.adapter.out.persistence.JdbcSemanticCacheAdapter;
import com.emme.assistant.ai.adapter.out.persistence.JdbcSemanticReferenceSearchAdapter;
import com.emme.assistant.ai.application.port.out.SemanticCachePort;
import com.emme.assistant.ai.application.semantic.EmbeddingVector;
import com.emme.assistant.ai.application.tool.AiToolResult;
import com.emme.assistant.ai.configuration.AiProperties;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("pgvector semantic integration")
class PgVectorSemanticIntegrationTest {

  private static final String IMAGE = "pgvector/pgvector:0.8.6-pg16-trixie";
  private static final String MODEL_NAME = EmbeddingModelDefaults.MODEL_NAME;
  private static final String MODEL_VERSION = EmbeddingModelDefaults.MODEL_VERSION;
  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID OTHER_TENANT_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse(IMAGE).asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("emme_test")
          .withUsername("emme")
          .withPassword("emme");

  private JdbcTemplate jdbc;
  private JdbcSemanticReferenceSearchAdapter semanticReferences;
  private JdbcSemanticCacheAdapter semanticCache;
  private JdbcAiToolIdempotencyStore toolIdempotency;

  @BeforeAll
  void connectToContainer() {
    DataSource dataSource =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    jdbc = new JdbcTemplate(dataSource);
    semanticReferences =
        new JdbcSemanticReferenceSearchAdapter(
            JdbcClient.create(dataSource),
            new AiProperties(
                "mock",
                null,
                new AiProperties.EmbeddingConfig(
                    MODEL_NAME, "http://localhost:11434", null, 768, MODEL_VERSION),
                true));
    semanticCache =
        new JdbcSemanticCacheAdapter(
            JdbcClient.create(dataSource),
            new AiProperties(
                "mock",
                null,
                new AiProperties.EmbeddingConfig(
                    MODEL_NAME, "http://localhost:11434", null, 768, MODEL_VERSION),
                true));
    toolIdempotency =
        new JdbcAiToolIdempotencyStore(JdbcClient.create(dataSource), new ObjectMapper());
  }

  @BeforeEach
  void createMinimalVectorSchema() {
    jdbc.execute("CREATE EXTENSION IF NOT EXISTS vector");
    jdbc.execute(
        """
        CREATE TABLE IF NOT EXISTS ai_intent_reference (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            tenant_id UUID NOT NULL,
            intent_key VARCHAR(80) NOT NULL,
            locale VARCHAR(10) NOT NULL,
            embedding vector(768),
            embedding_model_name VARCHAR(120),
            embedding_model_version VARCHAR(150),
            active BOOLEAN NOT NULL
        )
        """);
    jdbc.update("DELETE FROM ai_intent_reference");
    jdbc.execute(
        """
        CREATE TABLE IF NOT EXISTS ai_semantic_cache (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            tenant_id UUID NOT NULL,
            principal_id UUID NOT NULL,
            cache_kind VARCHAR(40) NOT NULL,
            query_text VARCHAR(4000) NOT NULL,
            context_fingerprint VARCHAR(128) NOT NULL,
            embedding vector(768) NOT NULL,
            embedding_model_name VARCHAR(120) NOT NULL,
            embedding_model_version VARCHAR(150) NOT NULL,
            prompt_version VARCHAR(150) NOT NULL,
            response_provider VARCHAR(120),
            response_model VARCHAR(160),
            knowledge_version VARCHAR(160),
            policy_version VARCHAR(160),
            source_version VARCHAR(160),
            response_payload JSONB NOT NULL,
            active BOOLEAN NOT NULL DEFAULT true,
            expires_at TIMESTAMPTZ NOT NULL,
            hit_count BIGINT NOT NULL DEFAULT 0,
            last_hit_at TIMESTAMPTZ,
            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            version BIGINT NOT NULL DEFAULT 0,
            write_idempotency_key VARCHAR(255) NOT NULL,
            UNIQUE (tenant_id, principal_id, write_idempotency_key)
        )
        """);
    jdbc.update("DELETE FROM ai_semantic_cache");
    jdbc.execute(
        """
        CREATE TABLE IF NOT EXISTS ai_tool_idempotency (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            tenant_id UUID NOT NULL,
            principal_id UUID NOT NULL,
            operation_key VARCHAR(320) NOT NULL,
            tool_key VARCHAR(120) NOT NULL,
            status VARCHAR(20) NOT NULL,
            result_payload JSONB,
            lease_expires_at TIMESTAMPTZ,
            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            version BIGINT NOT NULL DEFAULT 0,
            UNIQUE (tenant_id, principal_id, operation_key)
        )
        """);
    jdbc.update("DELETE FROM ai_tool_idempotency");
    jdbc.update(
        """
        INSERT INTO ai_intent_reference
            (id, tenant_id, intent_key, locale, embedding, embedding_model_name,
             embedding_model_version, active)
        VALUES (?, ?, ?, ?, CAST(? AS vector), ?, ?, true),
               (?, ?, ?, ?, CAST(? AS vector), ?, ?, true)
        """,
        UUID.randomUUID(),
        TENANT_ID,
        "QUOTE_DESIGN",
        "es-MX",
        "[1,0,0" + vectorPadding(),
        MODEL_NAME,
        MODEL_VERSION,
        UUID.randomUUID(),
        OTHER_TENANT_ID,
        "CHECK_AVAILABILITY",
        "es-MX",
        "[1,0,0" + vectorPadding(),
        MODEL_NAME,
        MODEL_VERSION);
  }

  @Test
  @DisplayName("searches pgvector with the authenticated tenant predicate")
  void searchesOnlyTheCurrentTenant() {
    EmbeddingVector query = new EmbeddingVector(MODEL_VERSION, vector(1.0f, 0.0f, 0.0f));
    AiExecutionContext context =
        new AiExecutionContext(
            TENANT_ID,
            PRINCIPAL_ID,
            Set.of("ROLE_CLIENT"),
            CONVERSATION_ID,
            WORKFLOW_ID,
            "trace-pgvector",
            "idempotency-pgvector");

    var matches =
        AiExecutionContextScope.call(
            context, () -> semanticReferences.searchIntents("es-MX", query, 10));

    assertThat(matches).extracting("key").containsExactly("QUOTE_DESIGN");
    assertThat(matches.getFirst().similarity()).isGreaterThan(0.99);
  }

  @Test
  @DisplayName("writes and reads a durable semantic cache entry in pgvector")
  void persistsAndCountsSemanticCacheHits() {
    EmbeddingVector query = new EmbeddingVector(MODEL_VERSION, vector(1.0f, 0.0f, 0.0f));
    SemanticCachePort.Put write =
        new SemanticCachePort.Put(
            "FAQ",
            "What are your hours?",
            "catalog-v1",
            "prompt-v1",
            "{\"answer\":\"We are open\"}",
            java.time.Instant.now().plusSeconds(60),
            query,
            "cache-integration-1");
    AiExecutionContext context =
        new AiExecutionContext(
            TENANT_ID,
            PRINCIPAL_ID,
            Set.of("ROLE_CLIENT"),
            CONVERSATION_ID,
            WORKFLOW_ID,
            "trace-cache",
            "idempotency-cache");

    UUID cacheId = AiExecutionContextScope.call(context, () -> semanticCache.put(write));
    var matches =
        AiExecutionContextScope.call(
            context,
            () ->
                semanticCache.find(
                    new SemanticCachePort.Lookup("FAQ", "catalog-v1", "prompt-v1", query), 2));

    assertThat(matches)
        .singleElement()
        .satisfies(
            candidate -> {
              assertThat(candidate.id()).isEqualTo(cacheId);
              assertThat(candidate.responsePayload()).contains("We are open");
              assertThat(candidate.similarity()).isGreaterThan(0.99);
            });
    assertThat(AiExecutionContextScope.call(context, () -> semanticCache.recordHit(cacheId)))
        .isTrue();
    assertThat(
            jdbc.queryForObject(
                "SELECT hit_count FROM ai_semantic_cache WHERE id = ?", Long.class, cacheId))
        .isEqualTo(1L);
  }

  @Test
  @DisplayName("persists mutation results and scopes replay by tenant")
  void persistsMutationResultAndPreventsReplayExecution() {
    AiExecutionContext context =
        new AiExecutionContext(
            TENANT_ID,
            PRINCIPAL_ID,
            Set.of("ROLE_CLIENT"),
            CONVERSATION_ID,
            WORKFLOW_ID,
            "trace-idempotency",
            "idempotency-tool");
    String operationKey = "createAppointment:" + PRINCIPAL_ID + ":idempotency-tool";
    AiToolResult stored = new AiToolResult("createAppointment", "appointment-created", true);

    assertThat(
            AiExecutionContextScope.call(
                context, () -> toolIdempotency.claim(operationKey, stored.toolKey())))
        .isTrue();
    AiExecutionContextScope.run(context, () -> toolIdempotency.complete(operationKey, stored));
    assertThat(AiExecutionContextScope.call(context, () -> toolIdempotency.find(operationKey)))
        .contains(stored);
    assertThat(
            AiExecutionContextScope.call(
                context, () -> toolIdempotency.claim(operationKey, stored.toolKey())))
        .isFalse();

    AiExecutionContext otherTenant =
        new AiExecutionContext(
            OTHER_TENANT_ID,
            PRINCIPAL_ID,
            Set.of("ROLE_CLIENT"),
            CONVERSATION_ID,
            WORKFLOW_ID,
            "trace-other-tenant",
            "idempotency-tool");
    assertThat(AiExecutionContextScope.call(otherTenant, () -> toolIdempotency.find(operationKey)))
        .isEmpty();
  }

  @Test
  @DisplayName("reclaims an expired in-progress mutation claim")
  void reclaimsExpiredMutationClaim() {
    AiExecutionContext context =
        new AiExecutionContext(
            TENANT_ID,
            PRINCIPAL_ID,
            Set.of("ROLE_CLIENT"),
            CONVERSATION_ID,
            WORKFLOW_ID,
            "trace-expired-claim",
            "idempotency-expired");
    String operationKey = "createAppointment:" + PRINCIPAL_ID + ":idempotency-expired";
    jdbc.update(
        """
        INSERT INTO ai_tool_idempotency
            (tenant_id, principal_id, operation_key, tool_key, status, lease_expires_at)
        VALUES (?, ?, ?, ?, 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL '1 minute')
        """,
        TENANT_ID,
        PRINCIPAL_ID,
        operationKey,
        "createAppointment");

    assertThat(
            AiExecutionContextScope.call(
                context, () -> toolIdempotency.claim(operationKey, "createAppointment")))
        .isTrue();
    assertThat(
            jdbc.queryForObject(
                "SELECT lease_expires_at > CURRENT_TIMESTAMP FROM ai_tool_idempotency WHERE tenant_id = ? AND principal_id = ? AND operation_key = ?",
                Boolean.class,
                TENANT_ID,
                PRINCIPAL_ID,
                operationKey))
        .isTrue();
  }

  private static String vectorPadding() {
    return "," + String.join(",", Collections.nCopies(765, "0")) + "]";
  }

  private static java.util.List<Float> vector(float... firstValues) {
    var values = new java.util.ArrayList<Float>(Collections.nCopies(768, 0.0f));
    for (int index = 0; index < firstValues.length; index++) {
      values.set(index, firstValues[index]);
    }
    return values;
  }
}
