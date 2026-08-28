package com.emme.assistant.ai.domain.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuoteWorkflowTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID PRINCIPAL_ID = UUID.randomUUID();
  private static final UUID CONVERSATION_ID = UUID.randomUUID();

  @Test
  void advancesOnlyThroughTheExplicitQuoteReviewLifecycle() {
    QuoteWorkflow workflow = received();

    workflow = workflow.transition(0, QuoteWorkflowState.EXTRACTING);
    workflow = workflow.transition(1, QuoteWorkflowState.QUOTE_CALCULATED);
    workflow = workflow.transition(2, QuoteWorkflowState.NEEDS_STAFF_REVIEW);
    workflow = workflow.transition(3, QuoteWorkflowState.WAITING_FOR_STAFF);
    workflow = workflow.transition(4, QuoteWorkflowState.STAFF_EDITED);
    workflow = workflow.transition(5, QuoteWorkflowState.QUOTE_READY);
    workflow = workflow.transition(6, QuoteWorkflowState.SENT_TO_CLIENT);

    assertThat(workflow.state()).isEqualTo(QuoteWorkflowState.SENT_TO_CLIENT);
    assertThat(workflow.version()).isEqualTo(7);
  }

  @Test
  void rejectsATransitionThatCouldSkipHumanReview() {
    QuoteWorkflow workflow = received().transition(0, QuoteWorkflowState.EXTRACTING);

    assertThatThrownBy(() -> workflow.transition(1, QuoteWorkflowState.QUOTE_READY))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Invalid quote workflow transition: EXTRACTING -> QUOTE_READY");
  }

  @Test
  void rejectsAStaleResumeInsteadOfOverwritingAReview() {
    QuoteWorkflow workflow = received().transition(0, QuoteWorkflowState.EXTRACTING);

    assertThatThrownBy(() -> workflow.transition(0, QuoteWorkflowState.QUOTE_CALCULATED))
        .isInstanceOf(StaleQuoteWorkflowVersionException.class)
        .hasMessage("Stale quote workflow version: expected 0 but was 1");
  }

  @Test
  void terminalWorkflowsCannotBeChanged() {
    QuoteWorkflow workflow =
        received()
            .transition(0, QuoteWorkflowState.EXTRACTING)
            .transition(1, QuoteWorkflowState.QUOTE_CALCULATED)
            .transition(2, QuoteWorkflowState.QUOTE_READY)
            .transition(3, QuoteWorkflowState.SENT_TO_CLIENT);

    assertThatThrownBy(() -> workflow.transition(4, QuoteWorkflowState.FAILED))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Invalid quote workflow transition: SENT_TO_CLIENT -> FAILED");
  }

  private static QuoteWorkflow received() {
    return QuoteWorkflow.received(
        UUID.randomUUID(), TENANT_ID, PRINCIPAL_ID, CONVERSATION_ID, "quote-request-1");
  }
}
