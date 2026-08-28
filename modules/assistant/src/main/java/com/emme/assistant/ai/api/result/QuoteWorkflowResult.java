package com.emme.assistant.ai.api.result;

import com.emme.assistant.ai.domain.quote.QuoteCalculation;
import com.emme.assistant.ai.domain.workflow.QuoteReviewTask;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflowState;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Application result that distinguishes a client-ready quote from a paused HITL workflow. */
public record QuoteWorkflowResult(
    UUID workflowId,
    QuoteWorkflowState state,
    Optional<QuoteCalculation> quote,
    Optional<QuoteReviewTask> reviewTask) {

  public QuoteWorkflowResult {
    Objects.requireNonNull(workflowId, "workflowId must not be null");
    Objects.requireNonNull(state, "state must not be null");
    Objects.requireNonNull(quote, "quote must not be null");
    Objects.requireNonNull(reviewTask, "reviewTask must not be null");
  }
}
