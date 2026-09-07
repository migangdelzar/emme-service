package com.emme.assistant.ai;

import static com.emme.testing.context.ExecutionTestContext.runWithContext;
import static com.emme.testing.context.ExecutionTestContext.withContext;
import static com.emme.testing.context.ExecutionTestContext.withTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.TestApplication;
import com.emme.assistant.ai.adapter.out.workflow.JdbcLangGraphCheckpointSaver;
import com.emme.assistant.ai.adapter.out.workflow.LangGraphQuoteWorkflowResumeAdapter;
import com.emme.assistant.ai.adapter.out.workflow.QuoteWorkflowGraph;
import com.emme.assistant.ai.adapter.out.workflow.TenantAwareCheckpointSaver;
import com.emme.assistant.ai.domain.workflow.QuoteReviewDecisionType;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.tenancy.adapter.out.client.database.TenantSchemaName;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.state.AgentState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(classes = TestApplication.class)
@PostgresIntegrationTest
class QuoteWorkflowResumeIntegrationTest {

  @Autowired
  @Qualifier("tenantJdbcClient")
  private JdbcClient jdbc;

  @Autowired
  @Qualifier("bootstrapJdbcClient")
  private JdbcClient bootstrapJdbc;

  @Autowired private ObjectMapper objectMapper;
  @Autowired private TenantProvisioningRepository provisioningRepository;
  @Autowired private TenantSchemaMigrationPort schemaMigrationPort;

  @Test
  void enforcesStaffAuthorizationAndTenantSchemaIsolationForResume() throws Exception {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    UUID owner = UUID.randomUUID();
    UUID conversation = UUID.randomUUID();
    UUID workflow = UUID.randomUUID();
    provisionTenant(tenantA);
    provisionTenant(tenantB);
    createConversation(tenantA, conversation, owner);

    AiExecutionContext ownerContext =
        context(tenantA, owner, conversation, workflow, "ROLE_CLIENT");
    CompiledGraph<AgentState> graph = graph();
    RunnableConfig config = RunnableConfig.builder().threadId(workflow + ":quote").build();
    AgentState paused =
        withContext(
            ownerContext, () -> graph.invoke(Map.of("needsReview", true), config).orElseThrow());
    assertThat(paused.<String>value("status")).contains("WAITING_FOR_STAFF");

    LangGraphQuoteWorkflowResumeAdapter adapter = new LangGraphQuoteWorkflowResumeAdapter(graph);
    assertThatThrownBy(
            () ->
                withContext(
                    ownerContext,
                    () -> {
                      adapter.resume(workflow, QuoteReviewDecisionType.APPROVED);
                      return null;
                    }))
        .isInstanceOf(SecurityException.class)
        .hasMessage("Staff role is required to resume a quote workflow");

    AiExecutionContext staffContext =
        context(tenantA, UUID.randomUUID(), conversation, workflow, "ROLE_tenant_staff");
    runWithContext(staffContext, () -> adapter.resume(workflow, QuoteReviewDecisionType.APPROVED));
    String status =
        withTenant(
            tenantA,
            () ->
                jdbc.sql("SELECT status FROM ai_workflow_run WHERE id = :workflowId")
                    .param("workflowId", workflow)
                    .query(String.class)
                    .single());
    assertThat(status).isEqualTo("QUOTE_READY");

    AiExecutionContext otherTenantStaff =
        context(tenantB, UUID.randomUUID(), conversation, workflow, "ROLE_tenant_staff");
    assertThatThrownBy(
            () ->
                withContext(
                    otherTenantStaff,
                    () -> {
                      adapter.resume(workflow, QuoteReviewDecisionType.APPROVED);
                      return null;
                    }))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unable to resume quote workflow: " + workflow)
        .hasRootCauseMessage("Quote workflow checkpoint not found");
  }

  private CompiledGraph<AgentState> graph() throws Exception {
    return new QuoteWorkflowGraph(
            new TenantAwareCheckpointSaver(new JdbcLangGraphCheckpointSaver(jdbc, objectMapper)))
        .compile();
  }

  private void provisionTenant(UUID tenantId) {
    String slug = "quote-resume-" + tenantId.toString().replace('-', 'a');
    String schemaName = TenantSchemaName.fromSlug(slug);
    bootstrapJdbc.sql("CREATE EXTENSION IF NOT EXISTS vector").update();
    provisioningRepository.requestProvisioning(tenantId, slug, schemaName);
    assertThat(schemaMigrationPort.migrate(tenantId, slug)).isEqualTo(schemaName);
  }

  private void createConversation(UUID tenantId, UUID conversationId, UUID participantId) {
    withTenant(
        tenantId,
        () ->
            jdbc.sql(
                    "INSERT INTO conversation (id, tenant_id, participant_id, channel) "
                        + "VALUES (:id, :tenantId, :participantId, 'WEB_CHAT')")
                .param("id", conversationId)
                .param("tenantId", tenantId)
                .param("participantId", participantId)
                .update());
  }

  private static AiExecutionContext context(
      UUID tenantId, UUID principalId, UUID conversationId, UUID workflowId, String role) {
    return new AiExecutionContext(
        tenantId,
        principalId,
        Set.of(role),
        conversationId,
        workflowId,
        "trace-quote-resume",
        "quote-resume-" + workflowId);
  }
}
