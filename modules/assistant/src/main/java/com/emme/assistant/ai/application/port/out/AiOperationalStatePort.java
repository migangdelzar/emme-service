package com.emme.assistant.ai.application.port.out;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Temporary operational state and lock boundary; durable workflow state remains in PostgreSQL. */
public interface AiOperationalStatePort {

  void save(WorkflowState state, Duration ttl);

  Optional<WorkflowState> load();

  boolean tryAcquireConversationLock(Duration lease);

  boolean releaseConversationLock();

  record WorkflowState(String status, String detail, Instant updatedAt, long version) {
    public WorkflowState {
      requireLabel(status, "status");
      if (detail == null) {
        detail = "";
      }
      if (detail.length() > 256) {
        throw new IllegalArgumentException("detail must not exceed 256 characters");
      }
      if (updatedAt == null) {
        throw new NullPointerException("updatedAt must not be null");
      }
      if (version < 0) {
        throw new IllegalArgumentException("version must not be negative");
      }
    }
  }

  static void requireLabel(String value, String field) {
    if (value == null || value.isBlank() || value.length() > 64 || !value.matches("[A-Z0-9_.-]+")) {
      throw new IllegalArgumentException(field + " must be a bounded uppercase label");
    }
  }
}
