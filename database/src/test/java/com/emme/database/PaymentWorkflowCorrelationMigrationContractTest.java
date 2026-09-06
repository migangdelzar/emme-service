package com.emme.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PaymentWorkflowCorrelationMigrationContractTest {

  private static final String MIGRATION =
      "db/emme-studio/releases/0.1.0/037-ai-workflow-correlations.sql";

  @Test
  void createsTenantScopedProviderToWorkflowCorrelation() throws IOException {
    assertThat(resource(MIGRATION))
        .contains("CREATE TABLE ai_payment_workflow_correlation")
        .contains("tenant_id UUID NOT NULL")
        .contains("workflow_id UUID NOT NULL")
        .contains("provider VARCHAR(80) NOT NULL")
        .contains("provider_reference VARCHAR(200) NOT NULL")
        .contains("UNIQUE (tenant_id, provider, provider_reference)")
        .contains("ENABLE ROW LEVEL SECURITY")
        .contains("tenant_isolation");
  }

  @Test
  void includesTheForwardMigrationInTheStudioChangelog() throws IOException {
    assertThat(resource("db/emme-studio/changelog.yaml"))
        .contains("releases/0.1.0/037-ai-workflow-correlations.sql");
  }

  private static String resource(String path) throws IOException {
    try (InputStream stream =
        PaymentWorkflowCorrelationMigrationContractTest.class
            .getClassLoader()
            .getResourceAsStream(path)) {
      assertThat(stream).as("classpath resource %s", path).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
