package com.emme.assistant.ai.domain.workflow;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Immutable HITL task with a single optimistic-lock-protected resolution. */
public record QuoteReviewTask(
    UUID id,
    UUID tenantId,
    UUID workflowId,
    QuoteReviewStatus status,
    UUID reviewerId,
    Optional<QuoteReviewDecisionType> decision,
    String notes,
    List<String> uncertaintyReasons,
    long version) {

  public QuoteReviewTask {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(tenantId, "tenantId must not be null");
    Objects.requireNonNull(workflowId, "workflowId must not be null");
    Objects.requireNonNull(status, "status must not be null");
    Objects.requireNonNull(decision, "decision must not be null");
    uncertaintyReasons = List.copyOf(Objects.requireNonNull(uncertaintyReasons));
    if (version < 0) {
      throw new IllegalArgumentException("version must not be negative");
    }
    if (status == QuoteReviewStatus.WAITING_FOR_STAFF || status == QuoteReviewStatus.CLAIMED) {
      if (decision.isPresent() || reviewerId != null) {
        throw new IllegalArgumentException("Open quote review tasks cannot have a decision");
      }
    } else if (decision.isEmpty() || reviewerId == null) {
      throw new IllegalArgumentException(
          "Resolved quote review tasks require a reviewer and decision");
    }
  }

  public static QuoteReviewTask waiting(
      UUID id, UUID tenantId, UUID workflowId, List<String> uncertaintyReasons) {
    return new QuoteReviewTask(
        id,
        tenantId,
        workflowId,
        QuoteReviewStatus.WAITING_FOR_STAFF,
        null,
        Optional.empty(),
        null,
        uncertaintyReasons,
        0);
  }

  public QuoteReviewTask resolve(
      long expectedVersion, UUID reviewerId, QuoteReviewDecisionType decision, String notes) {
    Objects.requireNonNull(reviewerId, "reviewerId must not be null");
    Objects.requireNonNull(decision, "decision must not be null");
    if (expectedVersion != version) {
      throw new StaleQuoteReviewVersionException(expectedVersion, version);
    }
    if (this.decision.isPresent()) {
      throw new IllegalStateException("Quote review task is already resolved: " + id);
    }
    QuoteReviewStatus nextStatus =
        switch (decision) {
          case APPROVED -> QuoteReviewStatus.APPROVED;
          case EDITED -> QuoteReviewStatus.EDITED;
          case REJECTED -> QuoteReviewStatus.REJECTED;
        };
    return new QuoteReviewTask(
        id,
        tenantId,
        workflowId,
        nextStatus,
        reviewerId,
        Optional.of(decision),
        notes,
        uncertaintyReasons,
        version + 1);
  }
}
