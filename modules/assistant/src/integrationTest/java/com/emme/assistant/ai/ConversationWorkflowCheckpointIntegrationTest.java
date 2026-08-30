package com.emme.assistant.ai;

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
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.kernel.type.ChannelType;
import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@SpringBootTest(classes = TestApplication.class)
@PostgresIntegrationTest
class ConversationWorkflowCheckpointIntegrationTest {

  private static final UUID SETUP_TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Autowired private JdbcClient jdbc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private StartConversationUseCase startConversation;
  @Autowired private DataSource dataSource;

  @BeforeEach
  void applyWorkflowMigrations() {
    TenantContextHolder.withTenantOverride(
        SETUP_TENANT,
        () -> {
          jdbc.sql(
                  """
                  CREATE OR REPLACE FUNCTION current_tenant_id()
                  RETURNS UUID
                  LANGUAGE sql
                  STABLE
                  AS 'SELECT nullif(current_setting(''app.current_tenant_id'', true), '''')::UUID'
                  """)
              .update();
          new ResourceDatabasePopulator(
                  new ClassPathResource("db/emme-studio/releases/0.1.0/016-ai-quote-workflow.sql"),
                  new ClassPathResource(
                      "db/emme-studio/releases/0.1.0/017-ai-workflow-checkpoint-next-node.sql"))
              .execute(dataSource);
        });
  }

  @Test
  void resumesAfterGraphRecreationAndRejectsAnotherTenant() throws Exception {
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    UUID principal = UUID.randomUUID();
    UUID conversation =
        startConversation
            .start(new StartConversationCommand(tenantA, UUID.randomUUID(), ChannelType.WEB_CHAT))
            .id();
    UUID workflow = UUID.randomUUID();
    AiExecutionContext tenantAContext = context(tenantA, principal, conversation, workflow);
    ProcessConversationCommand start =
        new ProcessConversationCommand(conversation, "please review", "workflow-checkpoint-turn");

    var paused = withContext(tenantAContext, () -> adapter().startOrResume(start, tenantAContext));
    assertThat(paused.status()).isEqualTo(ConversationWorkflowStatus.WAITING_FOR_APPROVAL);

    var resumed =
        withContext(
            tenantAContext,
            () ->
                adapter()
                    .resume(
                        new ResumeConversationWorkflowCommand(
                            workflow, conversation, ConversationWorkflowDecision.APPROVE),
                        tenantAContext));
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
    ConversationWorkflowCapabilities defaults = ConversationWorkflowCapabilities.defaults();
    return new ConversationWorkflowCapabilities(
        defaults.intentDetection(),
        defaults.decomposition(),
        defaults.semanticRouting(),
        defaults.slotExtraction(),
        defaults.retrieval(),
        defaults.toolExecution(),
        request -> new ConversationWorkflowCapabilities.WorkflowStep(Map.of(), true, false, null),
        defaults.responseComposition(),
        defaults.quoteWorkflow());
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

  private static <T> T withContext(AiExecutionContext context, ThrowingSupplier<T> action) {
    return TenantContextHolder.withTenantOverride(
        context.tenantId(), () -> AiExecutionContextScope.call(context, action::get));
  }

  @FunctionalInterface
  private interface ThrowingSupplier<T> {
    T get() throws Exception;
  }
}
