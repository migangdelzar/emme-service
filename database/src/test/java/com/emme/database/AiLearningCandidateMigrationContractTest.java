package com.emme.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AI learning candidate migration contract")
class AiLearningCandidateMigrationContractTest {

  private static final String MIGRATION =
      "db/emme-studio/releases/0.1.0/019-ai-learning-candidates.sql";
  private static final String EVALUATION_MIGRATION =
      "db/emme-studio/releases/0.1.0/020-ai-learning-evaluations.sql";

  @Test
  void definesTenantAndPrincipalScopedCandidates() throws IOException {
    String sql = migration();

    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS ai_learning_candidate");
    assertThat(sql).contains("tenant_id UUID NOT NULL");
    assertThat(sql).contains("principal_id UUID NOT NULL");
    assertThat(sql).contains("conversation_id UUID NOT NULL REFERENCES conversation(id)");
    assertThat(sql).contains("workflow_id UUID NOT NULL");
    assertThat(sql).contains("trace_id VARCHAR(128) NOT NULL");
    assertThat(sql).contains("reference_text VARCHAR(4000) NOT NULL");
    assertThat(sql).contains("evidence JSONB NOT NULL");
    assertThat(sql).contains("embedding_model_version VARCHAR(150) NOT NULL");
  }

  @Test
  void keepsCandidatesPendingEvaluationAndIdempotent() throws IOException {
    String sql = migration();

    assertThat(sql).contains("PENDING_EVALUATION");
    assertThat(sql).contains("APPROVED");
    assertThat(sql).contains("PROMOTED");
    assertThat(sql).contains("reference_fingerprint VARCHAR(64) NOT NULL");
    assertThat(sql)
        .containsPattern(
            "(?s)UNIQUE\\s*\\(\\s*tenant_id,\\s*principal_id,\\s*candidate_key,\\s*"
                + "reference_fingerprint,\\s*embedding_model_version\\s*\\)");
    assertThat(sql).contains("version BIGINT NOT NULL DEFAULT 0");
  }

  @Test
  void appliesTenantRlsAndIndexesCandidateReviewQueries() throws IOException {
    String sql = migration();

    assertThat(sql).contains("ALTER TABLE ai_learning_candidate ENABLE ROW LEVEL SECURITY");
    assertThat(sql).contains("CREATE POLICY tenant_isolation ON ai_learning_candidate");
    assertThat(sql).contains("tenant_id = current_tenant_id()");
    assertThat(sql).contains("idx_ai_learning_candidate_scope");
    assertThat(sql).contains("idx_ai_learning_candidate_status");
  }

  @Test
  void isIncludedByTheStudioChangelog() throws IOException {
    String changelog = resource("db/emme-studio/changelog.yaml");

    assertThat(changelog).contains("releases/0.1.0/019-ai-learning-candidates.sql");
    assertThat(changelog).contains("releases/0.1.0/020-ai-learning-evaluations.sql");
  }

  @Test
  void definesTenantScopedEvaluationEvidenceWithIdempotentVersions() throws IOException {
    String sql = resource(EVALUATION_MIGRATION);

    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS ai_learning_candidate_evaluation");
    assertThat(sql).contains("tenant_id UUID NOT NULL");
    assertThat(sql).contains("candidate_id UUID NOT NULL REFERENCES ai_learning_candidate(id)");
    assertThat(sql).contains("evaluation_version VARCHAR(150) NOT NULL");
    assertThat(sql).contains("metrics JSONB NOT NULL");
    assertThat(sql)
        .contains("UNIQUE (tenant_id, candidate_id, evaluation_version)")
        .contains("ALTER TABLE ai_learning_candidate_evaluation ENABLE ROW LEVEL SECURITY")
        .contains("tenant_id = current_tenant_id()")
        .contains("idx_ai_learning_candidate_evaluation_scope");
  }

  private static String migration() throws IOException {
    return resource(MIGRATION);
  }

  private static String resource(String path) throws IOException {
    try (InputStream stream =
        AiLearningCandidateMigrationContractTest.class.getClassLoader().getResourceAsStream(path)) {
      assertThat(stream).as("classpath resource %s", path).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
