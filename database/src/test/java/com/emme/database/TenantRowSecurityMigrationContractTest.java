package com.emme.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class TenantRowSecurityMigrationContractTest {

  private static final String MIGRATION =
      "db/emme-studio/releases/0.1.0/040-force-tenant-row-security.sql";

  private static final List<String> TENANT_TABLES =
      List.of(
          "business_profile",
          "operating_hours",
          "booking_policy",
          "customer",
          "channel_participant",
          "service",
          "artist",
          "artist_capability",
          "appointment",
          "subscription",
          "document",
          "document_chunk",
          "conversation",
          "conversation_event",
          "pending_action",
          "vector_projection",
          "projection_checkpoint",
          "notification",
          "payment",
          "calendar_sync_state",
          "calendar_event_link",
          "catalog_item",
          "catalog_item_image",
          "notification_preference",
          "studio_audit_event",
          "google_oauth_token",
          "google_spreadsheet_link",
          "payment_webhook_event",
          "whatsapp_webhook_event",
          "ai_workflow_run",
          "ai_workflow_checkpoint",
          "ai_extraction_result",
          "quote_draft",
          "quote_review_task",
          "quote_review_decision",
          "ai_intent_reference",
          "ai_tool_reference",
          "ai_semantic_cache",
          "ai_model_execution",
          "ai_tool_call",
          "ai_learning_candidate",
          "ai_learning_candidate_evaluation",
          "ai_age_graph_registry",
          "ai_semantic_execution",
          "ai_design_image",
          "ai_tool_idempotency",
          "ai_conversation_workflow_review_decision",
          "appointment_hold",
          "payment_link",
          "ai_payment_workflow_correlation");

  @Test
  void forcesTenantRowSecurityForEveryTenantDataTable() throws IOException {
    String sql = resource(MIGRATION);

    assertThat(sql).contains("-- changeset emme:040-force-tenant-row-security");
    for (String table : TENANT_TABLES) {
      assertThat(sql)
          .as("tenant table %s must be subject to RLS even for its owner", table)
          .contains("ALTER TABLE IF EXISTS " + table + " FORCE ROW LEVEL SECURITY");
    }
  }

  @Test
  void includesTheForwardMigrationInTheStudioChangelog() throws IOException {
    assertThat(resource("db/emme-studio/changelog.yaml"))
        .contains("releases/0.1.0/040-force-tenant-row-security.sql");
  }

  private static String resource(String path) throws IOException {
    try (InputStream stream =
        TenantRowSecurityMigrationContractTest.class.getClassLoader().getResourceAsStream(path)) {
      assertThat(stream).as("classpath resource %s", path).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
