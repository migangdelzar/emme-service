package com.emme.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AiPaymentWorkflowStatusMigrationContractTest {

  private static final String MIGRATION =
      "db/emme-studio/releases/0.1.0/039-ai-payment-workflow-status.sql";

  @Test
  void allowsPaymentWaitingStateInTheDurableWorkflowRun() throws IOException {
    assertThat(resource(MIGRATION))
        .contains("ALTER TABLE ai_workflow_run")
        .contains("WAITING_FOR_PAYMENT")
        .contains("ai_workflow_run_status_check");
  }

  @Test
  void includesTheForwardMigrationInTheStudioChangelog() throws IOException {
    assertThat(resource("db/emme-studio/changelog.yaml"))
        .contains("releases/0.1.0/039-ai-payment-workflow-status.sql");
  }

  private static String resource(String path) throws IOException {
    try (InputStream stream =
        AiPaymentWorkflowStatusMigrationContractTest.class
            .getClassLoader()
            .getResourceAsStream(path)) {
      assertThat(stream).as("classpath resource %s", path).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
