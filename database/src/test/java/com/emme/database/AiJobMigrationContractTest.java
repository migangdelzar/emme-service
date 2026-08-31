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
        .contains("ai_job_state", "tenant_id", "status", "attempts", "idx_ai_job_state_ready");
  }
}
