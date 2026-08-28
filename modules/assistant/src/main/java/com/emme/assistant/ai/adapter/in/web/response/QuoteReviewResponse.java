package com.emme.assistant.ai.adapter.in.web.response;

import com.emme.assistant.ai.api.result.ReviewQuoteResult;
import com.emme.assistant.ai.domain.workflow.QuoteReviewStatus;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflowState;
import java.util.Objects;
import java.util.UUID;

/** HTTP representation of the durable review and workflow state. */
public record QuoteReviewResponse(
    UUID reviewTaskId,
    UUID workflowId,
    QuoteReviewStatus reviewStatus,
    QuoteWorkflowState workflowState,
    long reviewVersion,
    long workflowVersion) {

  public QuoteReviewResponse {
    Objects.requireNonNull(reviewTaskId, "reviewTaskId must not be null");
    Objects.requireNonNull(workflowId, "workflowId must not be null");
    Objects.requireNonNull(reviewStatus, "reviewStatus must not be null");
    Objects.requireNonNull(workflowState, "workflowState must not be null");
  }

  public static QuoteReviewResponse from(ReviewQuoteResult result) {
    Objects.requireNonNull(result, "result must not be null");
    return new QuoteReviewResponse(
        result.reviewTask().id(),
        result.workflow().id(),
        result.reviewTask().status(),
        result.workflow().state(),
        result.reviewTask().version(),
        result.workflow().version());
  }
}
