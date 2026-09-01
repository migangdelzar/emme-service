package com.emme.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AI semantic search migration contract")
class AiSemanticSearchMigrationContractTest {

  private static final String MIGRATION =
      "db/emme-studio/releases/0.1.0/014-ai-semantic-search.sql";
  private static final String IDEMPOTENCY_MIGRATION =
      "db/emme-studio/releases/0.1.0/015-ai-semantic-cache-idempotency.sql";
  private static final String TRACE_MIGRATION =
      "db/emme-studio/releases/0.1.0/018-ai-execution-traces.sql";
  private static final String SEMANTIC_TRACE_MIGRATION =
      "db/emme-studio/releases/0.1.0/028-ai-semantic-execution-traces.sql";
  private static final String DIMENSION_MIGRATION =
      "db/emme-studio/releases/0.1.0/021-ai-embeddinggemma-dimension.sql";
  private static final String TOOL_IDEMPOTENCY_MIGRATION =
      "db/emme-studio/releases/0.1.0/022-ai-tool-idempotency.sql";
  private static final String TOOL_IDEMPOTENCY_LEASE_MIGRATION =
      "db/emme-studio/releases/0.1.0/023-ai-tool-idempotency-lease.sql";
  private static final String MODEL_NAME_MIGRATION =
      "db/emme-studio/releases/0.1.0/029-ai-embedding-model-name.sql";

  @Test
  void definesTenantScopedIntentAndToolReferenceTables() throws IOException {
    String sql = migration();

    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS ai_intent_reference");
    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS ai_tool_reference");
    assertThat(sql).contains("tenant_id UUID NOT NULL");
    assertThat(sql).contains("embedding vector(1024)");
    assertThat(sql).contains("embedding_model_version VARCHAR(150)");
    assertThat(sql).contains("CREATE INDEX IF NOT EXISTS idx_ai_intent_embedding");
    assertThat(sql).contains("CREATE INDEX IF NOT EXISTS idx_ai_tool_embedding");
  }

  @Test
  void definesPrincipalScopedExpiringSemanticCache() throws IOException {
    String sql = migration();

    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS ai_semantic_cache");
    assertThat(sql).contains("principal_id UUID NOT NULL");
    assertThat(sql).contains("expires_at TIMESTAMPTZ NOT NULL");
    assertThat(sql).contains("response_payload JSONB NOT NULL");
    assertThat(sql).contains("embedding vector(1024) NOT NULL");
    assertThat(sql).contains("CREATE INDEX IF NOT EXISTS idx_ai_cache_embedding");
    assertThat(sql).contains("CREATE INDEX IF NOT EXISTS idx_ai_cache_lookup");
  }

  @Test
  void appliesRlsPoliciesToEverySemanticTable() throws IOException {
    String sql = migration();

    assertThat(sql).contains("ALTER TABLE ai_intent_reference ENABLE ROW LEVEL SECURITY");
    assertThat(sql).contains("ALTER TABLE ai_tool_reference ENABLE ROW LEVEL SECURITY");
    assertThat(sql).contains("ALTER TABLE ai_semantic_cache ENABLE ROW LEVEL SECURITY");
    assertThat(sql)
        .containsPattern(
            "(?s)CREATE POLICY tenant_isolation ON ai_intent_reference.*tenant_id = current_tenant_id\\(\\)");
    assertThat(sql)
        .containsPattern(
            "(?s)CREATE POLICY tenant_isolation ON ai_tool_reference.*tenant_id = current_tenant_id\\(\\)");
    assertThat(sql)
        .containsPattern(
            "(?s)CREATE POLICY tenant_isolation ON ai_semantic_cache.*tenant_id = current_tenant_id\\(\\)");
  }

  @Test
  void isIncludedByTheStudioChangelog() throws IOException {
    String changelog = resource("db/emme-studio/changelog.yaml");

    assertThat(changelog).contains("releases/0.1.0/014-ai-semantic-search.sql");
    assertThat(changelog).contains("releases/0.1.0/015-ai-semantic-cache-idempotency.sql");
    assertThat(changelog).contains("releases/0.1.0/021-ai-embeddinggemma-dimension.sql");
  }

  @Test
  void addsAnIdempotencyKeyForDurableCacheWrites() throws IOException {
    String sql = resource(IDEMPOTENCY_MIGRATION);

    assertThat(sql).contains("ADD COLUMN IF NOT EXISTS write_idempotency_key VARCHAR(160)");
    assertThat(sql).contains("ALTER COLUMN write_idempotency_key SET NOT NULL");
    assertThat(sql).contains("idx_ai_cache_write_idempotency");
  }

  @Test
  void definesTenantScopedModelAndToolExecutionTraces() throws IOException {
    String sql = resource(TRACE_MIGRATION);

    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS ai_model_execution");
    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS ai_tool_call");
    assertThat(sql).contains("tenant_id UUID NOT NULL");
    assertThat(sql).contains("principal_id UUID NOT NULL");
    assertThat(sql).contains("conversation_id UUID NOT NULL");
    assertThat(sql).contains("workflow_id UUID NOT NULL");
    assertThat(sql).contains("trace_id VARCHAR(128) NOT NULL");
    assertThat(sql).contains("request_payload JSONB NOT NULL");
    assertThat(sql).contains("input_tokens INTEGER");
    assertThat(sql).contains("output_tokens INTEGER");
    assertThat(sql).contains("estimated_cost DECIMAL(18,8)");
    assertThat(sql).contains("arguments_payload JSONB NOT NULL");
    assertThat(sql).contains("CREATE INDEX IF NOT EXISTS idx_ai_model_execution_scope");
    assertThat(sql).contains("CREATE INDEX IF NOT EXISTS idx_ai_tool_call_scope");
  }

  @Test
  void appliesRlsAndIncludesTheTraceMigrationInTheStudioChangelog() throws IOException {
    String sql = resource(TRACE_MIGRATION);
    String changelog = resource("db/emme-studio/changelog.yaml");

    assertThat(sql).contains("ALTER TABLE ai_model_execution ENABLE ROW LEVEL SECURITY");
    assertThat(sql).contains("ALTER TABLE ai_tool_call ENABLE ROW LEVEL SECURITY");
    assertThat(sql).contains("CREATE POLICY tenant_isolation ON ai_model_execution");
    assertThat(sql).contains("CREATE POLICY tenant_isolation ON ai_tool_call");
    assertThat(changelog).contains("releases/0.1.0/018-ai-execution-traces.sql");
  }

  @Test
  void definesDurableTenantScopedSemanticOutcomeTraces() throws IOException {
    String sql = resource(SEMANTIC_TRACE_MIGRATION);

    assertThat(sql)
        .contains("CREATE TABLE IF NOT EXISTS ai_semantic_execution")
        .contains("top1_similarity")
        .contains("top2_similarity")
        .contains("margin")
        .contains("matches JSONB NOT NULL")
        .contains("dependency_version")
        .contains("invalidation_context")
        .contains("ALTER TABLE ai_semantic_execution ENABLE ROW LEVEL SECURITY");
    assertThat(resource("db/emme-studio/changelog.yaml"))
        .contains("releases/0.1.0/028-ai-semantic-execution-traces.sql");
  }

  @Test
  void persistsEmbeddingModelNameAcrossSemanticVectorTables() throws IOException {
    String sql = resource(MODEL_NAME_MIGRATION);

    assertThat(sql)
        .contains("ai_intent_reference")
        .contains("ai_tool_reference")
        .contains("ai_semantic_cache")
        .contains("ADD COLUMN IF NOT EXISTS embedding_model_name VARCHAR(120)")
        .contains("idx_ai_semantic_embedding_model_identity");
    assertThat(resource("db/emme-studio/changelog.yaml"))
        .contains("releases/0.1.0/029-ai-embedding-model-name.sql");
  }

  @Test
  void changesSemanticIndexesToTheEmbeddingGemmaDimensionWithoutTruncatingData()
      throws IOException {
    String sql = resource(DIMENSION_MIGRATION);

    assertThat(sql)
        .contains("ai_intent_reference")
        .contains("ai_tool_reference")
        .contains("ai_semantic_cache")
        .contains("vector(768)")
        .contains("RAISE EXCEPTION")
        .contains("existing embeddings must be reindexed");
  }

  @Test
  void definesTenantScopedMutationToolIdempotencyWithRls() throws IOException {
    String sql = resource(TOOL_IDEMPOTENCY_MIGRATION);

    assertThat(sql)
        .contains("CREATE TABLE IF NOT EXISTS ai_tool_idempotency")
        .contains("tenant_id UUID NOT NULL")
        .contains("principal_id UUID NOT NULL")
        .contains("operation_key VARCHAR(320) NOT NULL")
        .contains("status VARCHAR(20) NOT NULL")
        .contains("UNIQUE (tenant_id, principal_id, operation_key)")
        .contains("ALTER TABLE ai_tool_idempotency ENABLE ROW LEVEL SECURITY")
        .contains("CREATE POLICY tenant_isolation ON ai_tool_idempotency")
        .contains("current_tenant_id()");
  }

  @Test
  void includesMutationToolIdempotencyInTheStudioChangelog() throws IOException {
    assertThat(resource("db/emme-studio/changelog.yaml"))
        .contains("releases/0.1.0/022-ai-tool-idempotency.sql")
        .contains("releases/0.1.0/023-ai-tool-idempotency-lease.sql");
  }

  @Test
  void definesBoundedRecoveryForStaleMutationClaims() throws IOException {
    String sql = resource(TOOL_IDEMPOTENCY_LEASE_MIGRATION);

    assertThat(sql)
        .contains("ADD COLUMN IF NOT EXISTS lease_expires_at TIMESTAMPTZ")
        .contains("ai_tool_idempotency_in_progress_lease")
        .contains("idx_ai_tool_idempotency_expired_claims")
        .contains("status = 'IN_PROGRESS'");
  }

  private static String migration() throws IOException {
    return resource(MIGRATION);
  }

  private static String resource(String path) throws IOException {
    try (InputStream stream =
        AiSemanticSearchMigrationContractTest.class.getClassLoader().getResourceAsStream(path)) {
      assertThat(stream).as("classpath resource %s", path).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
