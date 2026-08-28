package com.emme.assistant.ai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.port.out.NailDesignExtractor;
import com.emme.assistant.ai.domain.quote.ArtComplexity;
import com.emme.assistant.ai.domain.quote.NailDesignFeatures;
import com.emme.assistant.ai.domain.quote.QuoteCalculation;
import com.emme.assistant.ai.domain.quote.QuoteLine;
import com.emme.assistant.ai.domain.quote.QuoteLineType;
import com.emme.assistant.ai.domain.workflow.QuoteDraft;
import com.emme.assistant.ai.domain.workflow.QuoteReviewDecisionType;
import com.emme.assistant.ai.domain.workflow.QuoteReviewStatus;
import com.emme.assistant.ai.domain.workflow.QuoteReviewTask;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflow;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

@SuppressWarnings("unchecked")
class JdbcQuotePersistenceAdapterTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();
  private static final UUID REVIEW_TASK_ID = UUID.randomUUID();

  @Test
  void findsAWorkflowUsingTheAuthenticatedTenantAndIdempotencyKey() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<QuoteWorkflow> result = mock(JdbcClient.MappedQuerySpec.class);
    stubQuery(jdbc, statement, result);
    QuoteWorkflow workflow = workflow(QuoteWorkflowStateHolder.RECEIVED, 0);
    when(result.list()).thenReturn(List.of(workflow));
    JdbcQuoteWorkflowRepository repository = new JdbcQuoteWorkflowRepository(jdbc);

    Optional<QuoteWorkflow> found =
        AiExecutionContextScope.call(context(), () -> repository.findByIdempotencyKey("idem-1"));

    assertThat(found).contains(workflow);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("FROM ai_workflow_run")
        .contains("tenant_id = :tenantId")
        .contains("idempotency_key = :idempotencyKey");
    verify(statement).param("tenantId", TENANT_ID);
    verify(statement).param("idempotencyKey", "idem-1");
  }

  @Test
  void savesAWorkflowWithAnOptimisticVersionPredicate() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<QuoteWorkflow> result = mock(JdbcClient.MappedQuerySpec.class);
    stubQuery(jdbc, statement, result);
    QuoteWorkflow workflow = workflow(QuoteWorkflowStateHolder.EXTRACTING, 1);
    when(result.single()).thenReturn(workflow);
    JdbcQuoteWorkflowRepository repository = new JdbcQuoteWorkflowRepository(jdbc);

    QuoteWorkflow saved = AiExecutionContextScope.call(context(), () -> repository.save(workflow));

    assertThat(saved).isEqualTo(workflow);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("UPDATE ai_workflow_run")
        .contains("version = :expectedVersion")
        .contains("RETURNING id");
    verify(statement).param("tenantId", TENANT_ID);
    verify(statement).param("workflowId", WORKFLOW_ID);
    verify(statement).param("expectedVersion", 0L);
  }

  @Test
  void savesExtractionUsingTenantWorkflowAndModelVersionMetadata() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.update()).thenReturn(1);
    JdbcQuoteArtifactRepository repository =
        new JdbcQuoteArtifactRepository(jdbc, new ObjectMapper());
    NailDesignExtractor.ExtractionResult extraction =
        new NailDesignExtractor.ExtractionResult(
            new NailDesignFeatures(
                null,
                null,
                "pink",
                List.of(),
                List.of(),
                null,
                null,
                null,
                ArtComplexity.SIMPLE,
                Map.of("artComplexity", 0.95),
                List.of(),
                false),
            "vision-v2",
            "prompt-v3",
            "schema-v1");

    AiExecutionContextScope.run(
        context(), () -> repository.saveExtraction(WORKFLOW_ID, extraction));

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc).sql(sql.capture());
    assertThat(sql.getValue())
        .contains("INSERT INTO ai_extraction_result")
        .contains("tenant_id")
        .contains("workflow_id")
        .contains("model_version")
        .contains("ON CONFLICT (tenant_id, workflow_id)");
    verify(statement).param("tenantId", TENANT_ID);
    verify(statement).param("workflowId", WORKFLOW_ID);
    verify(statement).param("modelVersion", "vision-v2");
  }

  @Test
  void savesDraftAndReviewTaskUsingTheAuthenticatedTenant() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<QuoteReviewTask> result = mock(JdbcClient.MappedQuerySpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.update()).thenReturn(1);
    when(statement.query(any(RowMapper.class))).thenReturn(result);
    QuoteReviewTask reviewTask =
        QuoteReviewTask.waiting(UUID.randomUUID(), TENANT_ID, WORKFLOW_ID, List.of("uncertain"));
    when(result.single()).thenReturn(reviewTask);
    JdbcQuoteArtifactRepository repository =
        new JdbcQuoteArtifactRepository(jdbc, new ObjectMapper());

    QuoteDraft draft =
        QuoteDraft.create(
            UUID.randomUUID(),
            TENANT_ID,
            WORKFLOW_ID,
            new QuoteCalculation(
                "base",
                "v1",
                "MXN",
                List.of(
                    new QuoteLine(
                        "base",
                        QuoteLineType.REQUIRED_SERVICE,
                        new BigDecimal("300"),
                        new BigDecimal("350"),
                        60)),
                new BigDecimal("300"),
                new BigDecimal("350"),
                60,
                true,
                List.of("uncertain")));

    AiExecutionContextScope.run(context(), () -> repository.saveDraft(draft));
    QuoteReviewTask savedTask =
        AiExecutionContextScope.call(context(), () -> repository.saveReviewTask(reviewTask));

    assertThat(savedTask).isEqualTo(reviewTask);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc, org.mockito.Mockito.atLeast(2)).sql(sql.capture());
    assertThat(sql.getAllValues())
        .anySatisfy(query -> assertThat(query).contains("INSERT INTO quote_draft"))
        .anySatisfy(query -> assertThat(query).contains("INSERT INTO quote_review_task"));
    verify(statement, org.mockito.Mockito.atLeastOnce()).param("tenantId", TENANT_ID);
  }

  @Test
  void rejectsArtifactWritesForAnotherTenant() {
    JdbcQuoteArtifactRepository repository =
        new JdbcQuoteArtifactRepository(mock(JdbcClient.class), new ObjectMapper());
    UUID foreignWorkflow = UUID.randomUUID();
    NailDesignExtractor.ExtractionResult extraction =
        new NailDesignExtractor.ExtractionResult(
            new NailDesignFeatures(
                null,
                null,
                null,
                List.of(),
                List.of(),
                null,
                null,
                null,
                ArtComplexity.SIMPLE,
                Map.of(),
                List.of(),
                false),
            "vision-v1",
            "prompt-v1",
            "schema-v1");

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.run(
                    context(), () -> repository.saveExtraction(foreignWorkflow, extraction)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("workflowId does not match AI execution context");
  }

  @Test
  void allowsStaffActorToPersistAWorkflowOwnedByAnotherPrincipalInTheSameTenant() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<QuoteWorkflow> result = mock(JdbcClient.MappedQuerySpec.class);
    stubQuery(jdbc, statement, result);
    QuoteWorkflow clientOwnedWorkflow =
        new QuoteWorkflow(
            WORKFLOW_ID,
            TENANT_ID,
            UUID.randomUUID(),
            CONVERSATION_ID,
            com.emme.assistant.ai.domain.workflow.QuoteWorkflowState.RECEIVED,
            "idem-1",
            0);
    when(result.single()).thenReturn(clientOwnedWorkflow);
    JdbcQuoteWorkflowRepository repository = new JdbcQuoteWorkflowRepository(jdbc);

    assertThat(AiExecutionContextScope.call(context(), () -> repository.save(clientOwnedWorkflow)))
        .isEqualTo(clientOwnedWorkflow);
  }

  @Test
  void findsAWorkflowByIdUsingTenantScopeWithoutRequiringRequestWorkflowCorrelation() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    JdbcClient.MappedQuerySpec<QuoteWorkflow> result = mock(JdbcClient.MappedQuerySpec.class);
    stubQuery(jdbc, statement, result);
    QuoteWorkflow workflow = workflow(QuoteWorkflowStateHolder.WAITING_FOR_STAFF, 2);
    when(result.list()).thenReturn(List.of(workflow));
    JdbcQuoteWorkflowRepository repository = new JdbcQuoteWorkflowRepository(jdbc);

    Optional<QuoteWorkflow> found =
        AiExecutionContextScope.call(
            contextWithWorkflow(UUID.randomUUID()), () -> repository.findById(WORKFLOW_ID));

    assertThat(found).contains(workflow);
  }

  @Test
  void resolvesAReviewWithTenantAndOptimisticVersionPredicates() {
    JdbcClient jdbc = mock(JdbcClient.class);
    JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class);
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.update()).thenReturn(1);
    JdbcQuoteReviewRepository repository = new JdbcQuoteReviewRepository(jdbc, new ObjectMapper());
    QuoteReviewTask resolved =
        new QuoteReviewTask(
            REVIEW_TASK_ID,
            TENANT_ID,
            WORKFLOW_ID,
            QuoteReviewStatus.APPROVED,
            PRINCIPAL_ID,
            Optional.of(QuoteReviewDecisionType.APPROVED),
            "Looks correct",
            List.of("uncertain"),
            1);

    QuoteReviewTask saved =
        AiExecutionContextScope.call(context(), () -> repository.save(resolved));

    assertThat(saved).isEqualTo(resolved);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbc, org.mockito.Mockito.times(2)).sql(sql.capture());
    assertThat(sql.getAllValues())
        .anySatisfy(
            query ->
                assertThat(query)
                    .contains("UPDATE quote_review_task")
                    .contains("version = :expectedVersion"))
        .anySatisfy(query -> assertThat(query).contains("INSERT INTO quote_review_decision"));
    verify(statement, org.mockito.Mockito.atLeastOnce()).param("tenantId", TENANT_ID);
    verify(statement, org.mockito.Mockito.atLeastOnce()).param("reviewTaskId", REVIEW_TASK_ID);
    verify(statement).param("expectedVersion", 0L);
    verify(statement, org.mockito.Mockito.atLeastOnce()).param("reviewerId", PRINCIPAL_ID);
  }

  private static QuoteWorkflow workflow(String state, long version) {
    return new QuoteWorkflow(
        WORKFLOW_ID,
        TENANT_ID,
        PRINCIPAL_ID,
        CONVERSATION_ID,
        com.emme.assistant.ai.domain.workflow.QuoteWorkflowState.valueOf(state),
        "idem-1",
        version);
  }

  private static AiExecutionContext context() {
    return contextWithWorkflow(WORKFLOW_ID);
  }

  private static AiExecutionContext contextWithWorkflow(UUID workflowId) {
    return new AiExecutionContext(
        TENANT_ID,
        PRINCIPAL_ID,
        Set.of("ROLE_CLIENT"),
        CONVERSATION_ID,
        workflowId,
        "trace-1",
        "idem-1");
  }

  private static void stubQuery(
      JdbcClient jdbc, JdbcClient.StatementSpec statement, JdbcClient.MappedQuerySpec<?> result) {
    when(jdbc.sql(anyString())).thenReturn(statement);
    when(statement.param(anyString(), any())).thenReturn(statement);
    when(statement.query(any(RowMapper.class))).thenReturn(result);
  }

  private static final class QuoteWorkflowStateHolder {
    private static final String RECEIVED = "RECEIVED";
    private static final String EXTRACTING = "EXTRACTING";
    private static final String WAITING_FOR_STAFF = "WAITING_FOR_STAFF";
  }
}
