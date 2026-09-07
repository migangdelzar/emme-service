package com.emme.assistant.ai;

import static com.emme.testing.context.ExecutionTestContext.withContext;
import static com.emme.testing.context.ExecutionTestContext.withTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.TestApplication;
import com.emme.assistant.ai.adapter.out.workflow.ConversationWorkflowGraph;
import com.emme.assistant.ai.adapter.out.workflow.JdbcLangGraphCheckpointSaver;
import com.emme.assistant.ai.adapter.out.workflow.LangGraphConversationWorkflowAdapter;
import com.emme.assistant.ai.adapter.out.workflow.TenantAwareCheckpointSaver;
import com.emme.assistant.ai.api.command.ProcessConversationCommand;
import com.emme.assistant.ai.api.command.ResumeConversationWorkflowCommand;
import com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowDecision;
import com.emme.assistant.ai.domain.workflow.ConversationWorkflowStatus;
import com.emme.assistant.api.command.StartConversationCommand;
import com.emme.assistant.api.usecase.StartConversationUseCase;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.type.ChannelType;
import com.emme.tenancy.adapter.out.client.database.TenantSchemaName;
import com.emme.tenancy.application.port.out.TenantProvisioningRepository;
import com.emme.tenancy.application.port.out.TenantSchemaMigrationPort;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(classes = TestApplication.class)
@PostgresIntegrationTest
class ConversationWorkflowCheckpointIntegrationTest {

  @Autowired
  @Qualifier("tenantJdbcClient")
  private JdbcClient jdbc;

  @Autowired
  @Qualifier("bootstrapJdbcClient")
  private JdbcClient bootstrapJdbc;

  @Autowired private ObjectMapper objectMapper;
  @Autowired private StartConversationUseCase startConversation;
  @Autowired private TenantProvisioningRepository provisioningRepository;
  @Autowired private TenantSchemaMigrationPort schemaMigrationPort;

  @Test
  void resumesAfterGraphRecreationAndRejectsAnotherTenant() throws Exception {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    UUID principal = UUID.randomUUID();
    provisionTenant(tenantA);
    provisionTenant(tenantB);
    UUID conversation =
        withTenant(
            tenantA,
            () ->
                startConversation
                    .start(
                        new StartConversationCommand(
                            tenantA, UUID.randomUUID(), ChannelType.WEB_CHAT))
                    .id());
    UUID workflow = UUID.randomUUID();
    AiExecutionContext tenantAContext = context(tenantA, principal, conversation, workflow);
    ProcessConversationCommand start =
        new ProcessConversationCommand(conversation, "please review", "workflow-checkpoint-turn");

    var paused = withContext(tenantAContext, () -> adapter().startOrResume(start, tenantAContext));
    assertThat(paused.status()).isEqualTo(ConversationWorkflowStatus.WAITING_FOR_APPROVAL);
    String persistedStatus =
        withTenant(
            tenantA,
            () ->
                jdbc.sql(
                        """
                        SELECT status
                        FROM ai_workflow_run
                        WHERE id = :workflowId
                          AND tenant_id = :tenantId
                          AND principal_id = :principalId
                          AND conversation_id = :conversationId
                        """)
                    .param("workflowId", workflow)
                    .param("tenantId", tenantA)
                    .param("principalId", principal)
                    .param("conversationId", conversation)
                    .query(String.class)
                    .single());
    assertThat(persistedStatus).isEqualTo("WAITING_FOR_APPROVAL");

    AiExecutionContext staffContext = staffContext(tenantA, conversation, workflow);
    var resumed =
        withContext(
            staffContext,
            () ->
                adapter()
                    .resume(
                        new ResumeConversationWorkflowCommand(
                            workflow, conversation, ConversationWorkflowDecision.APPROVE),
                        staffContext));
    assertThat(resumed.status()).isEqualTo(ConversationWorkflowStatus.SUCCEEDED);
    assertThat(resumed.tenantId()).isEqualTo(tenantA);
    assertThat(resumed.principalId()).isEqualTo(principal);

    AiExecutionContext tenantBContext = context(tenantB, principal, conversation, workflow);
    assertThatThrownBy(
            () -> withContext(tenantBContext, () -> adapter().startOrResume(start, tenantBContext)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Unable to start conversation workflow");
  }

  private LangGraphConversationWorkflowAdapter adapter() throws Exception {
    JdbcLangGraphCheckpointSaver saver = new JdbcLangGraphCheckpointSaver(jdbc, objectMapper);
    return new LangGraphConversationWorkflowAdapter(
        new ConversationWorkflowGraph(new TenantAwareCheckpointSaver(saver), approvalCapabilities())
            .compile());
  }

  private static ConversationWorkflowCapabilities approvalCapabilities() {
    ConversationWorkflowCapabilities.WorkflowStep empty =
        ConversationWorkflowCapabilities.WorkflowStep.empty();
    return new ConversationWorkflowCapabilities(
        request ->
            new ConversationWorkflowCapabilities.WorkflowStep(
                Map.of("intent", "GENERAL"), false, false, null),
        request -> empty,
        request ->
            new ConversationWorkflowCapabilities.WorkflowStep(
                Map.of("route", "GENERAL"), false, false, null),
        request -> empty,
        request -> empty,
        request -> empty,
        request -> new ConversationWorkflowCapabilities.WorkflowStep(Map.of(), true, false, null),
        request ->
            new ConversationWorkflowCapabilities.WorkflowStep(
                Map.of("response", "Your request is ready."), false, false, null),
        request -> empty);
  }

  private void provisionTenant(UUID tenantId) {
    String slug = "workflow-" + tenantId.toString().replace('-', 'a');
    String schemaName = TenantSchemaName.fromSlug(slug);
    bootstrapJdbc.sql("CREATE EXTENSION IF NOT EXISTS vector").update();
    provisioningRepository.requestProvisioning(tenantId, slug, schemaName);
    assertThat(schemaMigrationPort.migrate(tenantId, slug)).isEqualTo(schemaName);
  }

  private static AiExecutionContext context(
      UUID tenantId, UUID principalId, UUID conversationId, UUID workflowId) {
    return new AiExecutionContext(
        tenantId,
        principalId,
        Set.of("ROLE_CLIENT"),
        conversationId,
        workflowId,
        "trace-workflow-checkpoint",
        "workflow-checkpoint-turn");
  }

  private static AiExecutionContext staffContext(
      UUID tenantId, UUID conversationId, UUID workflowId) {
    return new AiExecutionContext(
        tenantId,
        UUID.randomUUID(),
        Set.of("ROLE_tenant_staff"),
        conversationId,
        workflowId,
        "trace-workflow-review",
        "workflow-review-turn");
  }
}
