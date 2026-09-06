package com.emme.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PaymentLinkMigrationContractTest {

  private static final String MIGRATION =
      "db/emme-studio/releases/0.1.0/036-ai-payment-workflow.sql";

  @Test
  void createsAnIdempotentTenantScopedPaymentLinkTable() throws IOException {
    assertThat(resource(MIGRATION))
        .contains("CREATE TABLE payment_link")
        .contains("tenant_id UUID NOT NULL")
        .contains("workflow_id UUID NOT NULL")
        .contains("provider VARCHAR(80) NOT NULL")
        .contains("checkout_url TEXT NOT NULL")
        .contains("expires_at TIMESTAMPTZ NOT NULL")
        .contains("idempotency_key VARCHAR(200) NOT NULL")
        .contains("UNIQUE (tenant_id, idempotency_key)")
        .contains("ENABLE ROW LEVEL SECURITY")
        .contains("tenant_isolation");
  }

  @Test
  void includesTheForwardMigrationInTheStudioChangelog() throws IOException {
    assertThat(resource("db/emme-studio/changelog.yaml"))
        .contains("releases/0.1.0/036-ai-payment-workflow.sql");
  }

  private static String resource(String path) throws IOException {
    try (InputStream stream =
        PaymentLinkMigrationContractTest.class.getClassLoader().getResourceAsStream(path)) {
      assertThat(stream).as("classpath resource %s", path).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
