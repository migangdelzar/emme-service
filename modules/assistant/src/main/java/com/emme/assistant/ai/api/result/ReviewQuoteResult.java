package com.emme.assistant.ai.api.result;

import com.emme.assistant.ai.domain.workflow.QuoteReviewTask;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflow;
import java.util.Objects;

/** Durable result of resolving a quote review and advancing its workflow. */
public record ReviewQuoteResult(QuoteReviewTask reviewTask, QuoteWorkflow workflow) {

  public ReviewQuoteResult {
    Objects.requireNonNull(reviewTask, "reviewTask must not be null");
    Objects.requireNonNull(workflow, "workflow must not be null");
  }
}
