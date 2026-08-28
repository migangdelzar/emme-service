package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.domain.workflow.QuoteReviewDecisionType;
import java.util.UUID;

/** Application boundary for resuming a persisted workflow after staff review. */
public interface QuoteWorkflowResumePort {

  void resume(UUID workflowId, QuoteReviewDecisionType decision);
}
