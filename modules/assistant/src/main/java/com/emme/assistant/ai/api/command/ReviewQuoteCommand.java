package com.emme.assistant.ai.api.command;

import com.emme.assistant.ai.domain.workflow.QuoteReviewDecisionType;
import java.util.Objects;
import java.util.UUID;

/** Backend command for a staff member to resolve a pending quote review. */
public record ReviewQuoteCommand(
    UUID reviewTaskId, long expectedVersion, QuoteReviewDecisionType decision, String notes) {

  public ReviewQuoteCommand {
    Objects.requireNonNull(reviewTaskId, "reviewTaskId must not be null");
    if (expectedVersion < 0) {
      throw new IllegalArgumentException("expectedVersion must not be negative");
    }
    Objects.requireNonNull(decision, "decision must not be null");
    if (notes != null && notes.length() > 4000) {
      throw new IllegalArgumentException("notes must not exceed 4000 characters");
    }
    notes = notes == null || notes.isBlank() ? null : notes;
  }
}
