package com.emme.assistant.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.assistant.ai.api.command.ReviewQuoteCommand;
import com.emme.assistant.ai.api.result.ReviewQuoteResult;
import com.emme.assistant.ai.application.port.out.QuoteReviewRepository;
import com.emme.assistant.ai.application.port.out.QuoteWorkflowRepository;
import com.emme.assistant.ai.application.port.out.QuoteWorkflowResumePort;
import com.emme.assistant.ai.domain.workflow.QuoteReviewDecisionType;
import com.emme.assistant.ai.domain.workflow.QuoteReviewTask;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflow;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflowState;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReviewQuoteServiceTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID STAFF_ID = UUID.randomUUID();
  private static final UUID CLIENT_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();
  private static final UUID WORKFLOW_ID = UUID.randomUUID();
  private static final UUID REVIEW_TASK_ID = UUID.randomUUID();

  @Test
  void approvesAReviewUsingTheAuthenticatedStaffIdentityAndResumesTheWorkflow() {
    RecordingReviews reviews =
        new RecordingReviews(
            QuoteReviewTask.waiting(
                REVIEW_TASK_ID, TENANT_ID, WORKFLOW_ID, java.util.List.of("uncertain")));
    RecordingWorkflows workflows = new RecordingWorkflows(waitingWorkflow());
    RecordingResumer resumer = new RecordingResumer();
    ReviewQuoteService service = new ReviewQuoteService(reviews, workflows, resumer);

    ReviewQuoteResult result =
        runAsStaff(
            () ->
                service.review(
                    new ReviewQuoteCommand(
                        REVIEW_TASK_ID, 0, QuoteReviewDecisionType.APPROVED, "Looks correct")));

    assertThat(result.reviewTask().decision()).contains(QuoteReviewDecisionType.APPROVED);
    assertThat(result.reviewTask().reviewerId()).isEqualTo(STAFF_ID);
    assertThat(result.workflow().state()).isEqualTo(QuoteWorkflowState.STAFF_APPROVED);
    assertThat(resumer.workflowId).isEqualTo(WORKFLOW_ID);
    assertThat(resumer.decision).isEqualTo(QuoteReviewDecisionType.APPROVED);
  }

  @Test
  void rejectsAReviewFromAClientContext() {
    ReviewQuoteService service =
        new ReviewQuoteService(
            new RecordingReviews(
                QuoteReviewTask.waiting(
                    REVIEW_TASK_ID, TENANT_ID, WORKFLOW_ID, java.util.List.of())),
            new RecordingWorkflows(waitingWorkflow()),
            new RecordingResumer());

    assertThatThrownBy(
            () ->
                runAsClient(
                    () ->
                        service.review(
                            new ReviewQuoteCommand(
                                REVIEW_TASK_ID, 0, QuoteReviewDecisionType.APPROVED, null))))
        .isInstanceOf(SecurityException.class)
        .hasMessage("Staff role is required to review a quote");
  }

  @Test
  void propagatesOptimisticLockFailureFromTheReviewTask() {
    QuoteReviewTask task =
        new QuoteReviewTask(
            REVIEW_TASK_ID,
            TENANT_ID,
            WORKFLOW_ID,
            com.emme.assistant.ai.domain.workflow.QuoteReviewStatus.WAITING_FOR_STAFF,
            null,
            Optional.empty(),
            null,
            java.util.List.of("uncertain"),
            2);
    ReviewQuoteService service =
        new ReviewQuoteService(
            new RecordingReviews(task),
            new RecordingWorkflows(waitingWorkflow()),
            new RecordingResumer());

    assertThatThrownBy(
            () ->
                runAsStaff(
                    () ->
                        service.review(
                            new ReviewQuoteCommand(
                                REVIEW_TASK_ID, 1, QuoteReviewDecisionType.APPROVED, null))))
        .isInstanceOf(com.emme.assistant.ai.domain.workflow.StaleQuoteReviewVersionException.class);
  }

  @Test
  void resolvesTheWorkflowCorrelationBeforePersistingAReviewFromAnInboundRequest() {
    ReviewQuoteService service =
        new ReviewQuoteService(
            new RecordingReviews(
                QuoteReviewTask.waiting(
                    REVIEW_TASK_ID, TENANT_ID, WORKFLOW_ID, java.util.List.of("uncertain"))),
            new RecordingWorkflows(waitingWorkflow()),
            new RecordingResumer());
    UUID inboundCorrelation = UUID.randomUUID();
    AiExecutionContext inboundContext =
        new AiExecutionContext(
            TENANT_ID,
            STAFF_ID,
            Set.of("tenant_staff"),
            inboundCorrelation,
            inboundCorrelation,
            "trace-inbound",
            "review-inbound");

    ReviewQuoteResult result =
        AiExecutionContextScope.call(
            inboundContext,
            () ->
                service.review(
                    new ReviewQuoteCommand(
                        REVIEW_TASK_ID, 0, QuoteReviewDecisionType.APPROVED, null)));

    assertThat(result.workflow().id()).isEqualTo(WORKFLOW_ID);
    assertThat(result.workflow().state()).isEqualTo(QuoteWorkflowState.STAFF_APPROVED);
  }

  private static QuoteWorkflow waitingWorkflow() {
    return new QuoteWorkflow(
        WORKFLOW_ID,
        TENANT_ID,
        CLIENT_ID,
        CONVERSATION_ID,
        QuoteWorkflowState.WAITING_FOR_STAFF,
        "idem-1",
        0);
  }

  private static <T> T runAsStaff(CheckedSupplier<T> action) {
    return run(
        new AiExecutionContext(
            TENANT_ID,
            STAFF_ID,
            Set.of("tenant_staff"),
            CONVERSATION_ID,
            WORKFLOW_ID,
            "trace-staff",
            "review-1"),
        action);
  }

  private static <T> T runAsClient(CheckedSupplier<T> action) {
    return run(
        new AiExecutionContext(
            TENANT_ID,
            CLIENT_ID,
            Set.of("tenant_client"),
            CONVERSATION_ID,
            WORKFLOW_ID,
            "trace-client",
            "review-1"),
        action);
  }

  private static <T> T run(AiExecutionContext context, CheckedSupplier<T> action) {
    return AiExecutionContextScope.call(context, action::get);
  }

  private interface CheckedSupplier<T> {
    T get();
  }

  private static final class RecordingReviews implements QuoteReviewRepository {
    private final QuoteReviewTask task;
    private QuoteReviewTask saved;

    private RecordingReviews(QuoteReviewTask task) {
      this.task = task;
    }

    @Override
    public Optional<QuoteReviewTask> findById(UUID reviewTaskId) {
      return Optional.of(task);
    }

    @Override
    public QuoteReviewTask save(QuoteReviewTask reviewTask) {
      saved = reviewTask;
      return reviewTask;
    }
  }

  private static final class RecordingWorkflows implements QuoteWorkflowRepository {
    private final QuoteWorkflow workflow;
    private QuoteWorkflow saved;

    private RecordingWorkflows(QuoteWorkflow workflow) {
      this.workflow = workflow;
    }

    @Override
    public Optional<QuoteWorkflow> findByIdempotencyKey(String idempotencyKey) {
      return Optional.empty();
    }

    @Override
    public Optional<QuoteWorkflow> findById(UUID workflowId) {
      return Optional.of(workflow);
    }

    @Override
    public QuoteWorkflow save(QuoteWorkflow quoteWorkflow) {
      saved = quoteWorkflow;
      return quoteWorkflow;
    }
  }

  private static final class RecordingResumer implements QuoteWorkflowResumePort {
    private UUID workflowId;
    private QuoteReviewDecisionType decision;

    @Override
    public void resume(UUID workflowId, QuoteReviewDecisionType decision) {
      this.workflowId = workflowId;
      this.decision = decision;
    }
  }
}
