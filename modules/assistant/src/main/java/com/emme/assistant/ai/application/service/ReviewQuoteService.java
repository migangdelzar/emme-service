package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.api.command.ReviewQuoteCommand;
import com.emme.assistant.ai.api.result.ReviewQuoteResult;
import com.emme.assistant.ai.api.usecase.ReviewQuoteUseCase;
import com.emme.assistant.ai.application.port.out.QuoteReviewRepository;
import com.emme.assistant.ai.application.port.out.QuoteWorkflowRepository;
import com.emme.assistant.ai.application.port.out.QuoteWorkflowResumePort;
import com.emme.assistant.ai.domain.workflow.QuoteReviewDecisionType;
import com.emme.assistant.ai.domain.workflow.QuoteReviewTask;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflow;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflowState;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves a staff quote review and resumes the corresponding workflow. */
@Service
@Transactional
@ConditionalOnProperty(prefix = "app.ai.quote", name = "enabled", havingValue = "true")
public class ReviewQuoteService implements ReviewQuoteUseCase {

  private static final Set<String> STAFF_ROLES =
      Set.of(
          "tenant_staff",
          "tenant_owner",
          "ROLE_tenant_staff",
          "ROLE_tenant_owner",
          "ROLE_STAFF",
          "ROLE_OWNER",
          "ROLE_ADMIN",
          "ROLE_admin",
          "admin");

  private final QuoteReviewRepository reviews;
  private final QuoteWorkflowRepository workflows;
  private final QuoteWorkflowResumePort resumer;

  public ReviewQuoteService(
      QuoteReviewRepository reviews,
      QuoteWorkflowRepository workflows,
      QuoteWorkflowResumePort resumer) {
    this.reviews = Objects.requireNonNull(reviews, "reviews must not be null");
    this.workflows = Objects.requireNonNull(workflows, "workflows must not be null");
    this.resumer = Objects.requireNonNull(resumer, "resumer must not be null");
  }

  @Override
  public ReviewQuoteResult review(ReviewQuoteCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    if (context.roles().stream().noneMatch(STAFF_ROLES::contains)) {
      throw new SecurityException("Staff role is required to review a quote");
    }

    QuoteReviewTask task =
        reviews
            .findById(command.reviewTaskId())
            .orElseThrow(() -> new IllegalArgumentException("Quote review task not found"));
    requireTenant(task, context);
    QuoteWorkflow workflow =
        workflows
            .findById(task.workflowId())
            .orElseThrow(() -> new IllegalArgumentException("Quote workflow not found"));
    requireTenant(workflow, context);

    AiExecutionContext workflowContext =
        context.withWorkflow(workflow.conversationId(), workflow.id());
    return AiExecutionContextScope.call(workflowContext, () -> resolve(command, task, workflow));
  }

  private ReviewQuoteResult resolve(
      ReviewQuoteCommand command, QuoteReviewTask task, QuoteWorkflow workflow) {
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();

    QuoteReviewTask resolved =
        task.resolve(
            command.expectedVersion(), context.principalId(), command.decision(), command.notes());
    QuoteWorkflowState nextState = nextState(command.decision());
    QuoteWorkflow advanced = workflow.transition(workflow.version(), nextState);

    QuoteReviewTask savedTask = reviews.save(resolved);
    QuoteWorkflow savedWorkflow = workflows.save(advanced);
    resumer.resume(savedWorkflow.id(), command.decision());
    return new ReviewQuoteResult(savedTask, savedWorkflow);
  }

  private static QuoteWorkflowState nextState(QuoteReviewDecisionType decision) {
    return switch (decision) {
      case APPROVED -> QuoteWorkflowState.STAFF_APPROVED;
      case EDITED -> QuoteWorkflowState.STAFF_EDITED;
      case REJECTED -> QuoteWorkflowState.FAILED;
    };
  }

  private static void requireTenant(QuoteReviewTask task, AiExecutionContext context) {
    if (!context.tenantId().equals(task.tenantId())) {
      throw new IllegalArgumentException("tenantId does not match AI execution context");
    }
  }

  private static void requireTenant(QuoteWorkflow workflow, AiExecutionContext context) {
    if (!context.tenantId().equals(workflow.tenantId())) {
      throw new IllegalArgumentException("tenantId does not match AI execution context");
    }
  }
}
