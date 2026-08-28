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
  }

  @Test
  void addsAnIdempotencyKeyForDurableCacheWrites() throws IOException {
    String sql = resource(IDEMPOTENCY_MIGRATION);

    assertThat(sql).contains("ADD COLUMN IF NOT EXISTS write_idempotency_key VARCHAR(160)");
    assertThat(sql).contains("ALTER COLUMN write_idempotency_key SET NOT NULL");
    assertThat(sql).contains("idx_ai_cache_write_idempotency");
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
