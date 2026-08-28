package com.emme.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AI quote workflow migration contract")
class AiQuoteWorkflowMigrationContractTest {

  private static final String MIGRATION = "db/emme-studio/releases/0.1.0/016-ai-quote-workflow.sql";
  private static final String CHECKPOINT_MIGRATION =
      "db/emme-studio/releases/0.1.0/017-ai-workflow-checkpoint-next-node.sql";

  @Test
  void definesDurableWorkflowAndQuoteArtifacts() throws IOException {
    String sql = migration();

    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS ai_workflow_run");
    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS ai_workflow_checkpoint");
    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS ai_extraction_result");
    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS quote_draft");
    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS quote_review_task");
    assertThat(sql).contains("CREATE TABLE IF NOT EXISTS quote_review_decision");
    assertThat(sql).contains("principal_id UUID NOT NULL");
    assertThat(sql).contains("model_version VARCHAR(150) NOT NULL");
    assertThat(sql).contains("prompt_version VARCHAR(150) NOT NULL");
    assertThat(sql).contains("template_version VARCHAR(150) NOT NULL");
    assertThat(sql).contains("attributes JSONB NOT NULL");
  }

  @Test
  void givesWorkflowCommandsAndReviewsDurableIdempotencyAndVersioning() throws IOException {
    String sql = migration();

    assertThat(sql).contains("idempotency_key VARCHAR(160) NOT NULL");
    assertThat(sql).contains("UNIQUE (tenant_id, idempotency_key)");
    assertThat(sql).contains("version BIGINT NOT NULL DEFAULT 0");
    assertThat(sql).contains("UNIQUE (tenant_id, review_task_id, decision_version)");
  }

  @Test
  void appliesTenantRlsToEveryQuoteWorkflowTable() throws IOException {
    String sql = migration();

    for (String table :
        new String[] {
          "ai_workflow_run",
          "ai_workflow_checkpoint",
          "ai_extraction_result",
          "quote_draft",
          "quote_review_task",
          "quote_review_decision"
        }) {
      assertThat(sql).contains("ALTER TABLE " + table + " ENABLE ROW LEVEL SECURITY");
      assertThat(sql)
          .containsPattern(
              "(?s)CREATE POLICY tenant_isolation ON "
                  + table
                  + ".*tenant_id = current_tenant_id\\(\\)");
    }
  }

  @Test
  void isIncludedByTheStudioChangelog() throws IOException {
    String changelog = resource("db/emme-studio/changelog.yaml");

    assertThat(changelog).contains("releases/0.1.0/016-ai-quote-workflow.sql");
    assertThat(changelog).contains("releases/0.1.0/017-ai-workflow-checkpoint-next-node.sql");
  }

  @Test
  void storesTheNextNodeRequiredForDurableLangGraphResume() throws IOException {
    String sql = resource(CHECKPOINT_MIGRATION);

    assertThat(sql).contains("ALTER TABLE ai_workflow_checkpoint");
    assertThat(sql).contains("ADD COLUMN IF NOT EXISTS next_node_name VARCHAR(120)");
  }

  private static String migration() throws IOException {
    return resource(MIGRATION);
  }

  private static String resource(String path) throws IOException {
    try (InputStream stream =
        AiQuoteWorkflowMigrationContractTest.class.getClassLoader().getResourceAsStream(path)) {
      assertThat(stream).as("classpath resource %s", path).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
