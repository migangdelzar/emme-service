package com.emme.assistant.ai.domain.workflow;

import java.util.Objects;
import java.util.UUID;

/** Immutable quote workflow aggregate with explicit transitions and optimistic versioning. */
public record QuoteWorkflow(
    UUID id,
    UUID tenantId,
    UUID principalId,
    UUID conversationId,
    QuoteWorkflowState state,
    String idempotencyKey,
    long version) {

  public QuoteWorkflow {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(tenantId, "tenantId must not be null");
    Objects.requireNonNull(principalId, "principalId must not be null");
    Objects.requireNonNull(conversationId, "conversationId must not be null");
    Objects.requireNonNull(state, "state must not be null");
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new IllegalArgumentException("idempotencyKey must not be blank");
    }
    if (version < 0) {
      throw new IllegalArgumentException("version must not be negative");
    }
  }

  public static QuoteWorkflow received(
      UUID id, UUID tenantId, UUID principalId, UUID conversationId, String idempotencyKey) {
    return new QuoteWorkflow(
        id, tenantId, principalId, conversationId, QuoteWorkflowState.RECEIVED, idempotencyKey, 0);
  }

  public QuoteWorkflow transition(long expectedVersion, QuoteWorkflowState nextState) {
    Objects.requireNonNull(nextState, "nextState must not be null");
    if (expectedVersion != version) {
      throw new StaleQuoteWorkflowVersionException(expectedVersion, version);
    }
    if (!isAllowed(state, nextState)) {
      throw new IllegalStateException(
          "Invalid quote workflow transition: " + state + " -> " + nextState);
    }
    return new QuoteWorkflow(
        id, tenantId, principalId, conversationId, nextState, idempotencyKey, version + 1);
  }

  private static boolean isAllowed(QuoteWorkflowState current, QuoteWorkflowState next) {
    if (next == QuoteWorkflowState.FAILED) {
      return current != QuoteWorkflowState.FAILED && current != QuoteWorkflowState.SENT_TO_CLIENT;
    }
    return switch (current) {
      case RECEIVED -> next == QuoteWorkflowState.EXTRACTING;
      case EXTRACTING ->
          next == QuoteWorkflowState.QUOTE_CALCULATED
              || next == QuoteWorkflowState.NEEDS_STAFF_REVIEW;
      case QUOTE_CALCULATED ->
          next == QuoteWorkflowState.NEEDS_STAFF_REVIEW || next == QuoteWorkflowState.QUOTE_READY;
      case NEEDS_STAFF_REVIEW -> next == QuoteWorkflowState.WAITING_FOR_STAFF;
      case WAITING_FOR_STAFF ->
          next == QuoteWorkflowState.STAFF_APPROVED || next == QuoteWorkflowState.STAFF_EDITED;
      case STAFF_APPROVED, STAFF_EDITED -> next == QuoteWorkflowState.QUOTE_READY;
      case QUOTE_READY -> next == QuoteWorkflowState.SENT_TO_CLIENT;
      case SENT_TO_CLIENT, FAILED -> false;
    };
  }
}
