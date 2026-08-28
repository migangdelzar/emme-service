package com.emme.assistant.ai.domain.workflow;

/** Durable review-task state. */
public enum QuoteReviewStatus {
  WAITING_FOR_STAFF,
  CLAIMED,
  APPROVED,
  EDITED,
  REJECTED
}
