package com.emme.assistant.ai.domain.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuoteReviewTaskTest {

  @Test
  void recordsAnExplicitStaffApprovalAndAdvancesTheReviewVersion() {
    QuoteReviewTask task = waiting();
    UUID reviewerId = UUID.randomUUID();

    QuoteReviewTask reviewed =
        task.resolve(0, reviewerId, QuoteReviewDecisionType.APPROVED, "Looks correct");

    assertThat(reviewed.status()).isEqualTo(QuoteReviewStatus.APPROVED);
    assertThat(reviewed.reviewerId()).isEqualTo(reviewerId);
    assertThat(reviewed.version()).isEqualTo(1);
    assertThat(reviewed.decision()).contains(QuoteReviewDecisionType.APPROVED);
  }

  @Test
  void recordsAnEditAsADistinctHumanDecision() {
    QuoteReviewTask reviewed =
        waiting()
            .resolve(
                0, UUID.randomUUID(), QuoteReviewDecisionType.EDITED, "Use the salon template");

    assertThat(reviewed.status()).isEqualTo(QuoteReviewStatus.EDITED);
    assertThat(reviewed.decision()).contains(QuoteReviewDecisionType.EDITED);
    assertThat(reviewed.notes()).contains("Use the salon template");
  }

  @Test
  void rejectsAStaleReviewCommand() {
    QuoteReviewTask task =
        waiting().resolve(0, UUID.randomUUID(), QuoteReviewDecisionType.APPROVED, "ok");

    assertThatThrownBy(
            () -> task.resolve(0, UUID.randomUUID(), QuoteReviewDecisionType.EDITED, "late edit"))
        .isInstanceOf(StaleQuoteReviewVersionException.class)
        .hasMessage("Stale quote review version: expected 0 but was 1");
  }

  @Test
  void requiresAReviewerAndDoesNotAllowASecondDecision() {
    QuoteReviewTask task = waiting();

    assertThatThrownBy(
            () -> task.resolve(0, null, QuoteReviewDecisionType.APPROVED, "missing reviewer"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("reviewerId must not be null");

    QuoteReviewTask resolved =
        task.resolve(0, UUID.randomUUID(), QuoteReviewDecisionType.REJECTED, "unclear image");
    assertThatThrownBy(
            () -> resolved.resolve(1, UUID.randomUUID(), QuoteReviewDecisionType.APPROVED, "again"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Quote review task is already resolved: " + resolved.id());
  }

  private static QuoteReviewTask waiting() {
    return QuoteReviewTask.waiting(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        List.of("extension type is unclear"));
  }
}
