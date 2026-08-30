package com.emme.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ConversationEventIdempotencyMigrationContractTest {

  private static final String MIGRATION =
      "db/emme-studio/releases/0.1.0/025-conversation-event-idempotency.sql";

  @Test
  void createsAPrincipalScopedConversationEventIdempotencyMarkerWithoutChangingRls()
      throws IOException {
    String sql = resource(MIGRATION);

    assertThat(sql)
        .contains("ALTER TABLE conversation_event")
        .contains("ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255)")
        .contains("ADD COLUMN IF NOT EXISTS idempotency_principal_id UUID")
        .contains("CREATE UNIQUE INDEX IF NOT EXISTS idx_conversation_event_idempotency")
        .contains("tenant_id,")
        .contains("idempotency_principal_id,")
        .contains("conversation_id,")
        .contains("event_type,")
        .contains("idempotency_key\n")
        .contains("WHERE idempotency_key IS NOT NULL");
  }

  @Test
  void includesTheMarkerMigrationInTheStudioChangelog() throws IOException {
    assertThat(resource("db/emme-studio/changelog.yaml"))
        .contains("releases/0.1.0/025-conversation-event-idempotency.sql");
  }

  private static String resource(String path) throws IOException {
    try (InputStream stream =
        ConversationEventIdempotencyMigrationContractTest.class
            .getClassLoader()
            .getResourceAsStream(path)) {
      assertThat(stream).as("classpath resource %s", path).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
