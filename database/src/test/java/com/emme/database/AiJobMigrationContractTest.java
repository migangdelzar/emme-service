package com.emme.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AiJobMigrationContractTest {
  @Test
  void declaresDurableTenantScopedJobStateAndReadyIndex() throws Exception {
    String sql =
        Files.readString(
            Path.of("src/main/resources/db/emme-studio/releases/0.1.0/028-ai-job-state.sql"));
    assertThat(sql)
        .contains(
            "CREATE TABLE IF NOT EXISTS emme_core.ai_job_state",
            "job_id UUID PRIMARY KEY",
            "tenant_id UUID NOT NULL",
            "attempts INTEGER NOT NULL DEFAULT 0",
            "CHECK (status IN ('QUEUED','CLAIMED','COMPLETED','RETRYING','DEAD_LETTER'))",
            "idx_ai_job_state_ready",
            "FOR ALL USING (tenant_id = current_tenant_id())",
            "ALTER TABLE emme_core.ai_job_state FORCE ROW LEVEL SECURITY");
  }

  @Test
  void declaresAllDurableContextColumnsAndRetryIndexes() throws Exception {
    String sql =
        Files.readString(
            Path.of("src/main/resources/db/emme-studio/releases/0.1.0/028-ai-job-state.sql"));
    assertThat(sql)
        .contains(
            "principal_id",
            "conversation_id",
            "workflow_id",
            "trace_id",
            "idempotency_key",
            "idx_ai_job_state_claimed");
  }

  @Test
  void documentsPostgresOnlyStatementsForLiveIntegrationCoverage() throws Exception {
    String sql =
        Files.readString(
            Path.of("src/main/resources/db/emme-studio/releases/0.1.0/028-ai-job-state.sql"));
    assertThat(sql).contains("DO $$", "ENABLE ROW LEVEL SECURITY", "current_tenant_id()");
  }
}
