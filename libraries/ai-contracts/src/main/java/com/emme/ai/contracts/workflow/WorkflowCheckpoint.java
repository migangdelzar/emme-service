package com.emme.ai.contracts.workflow;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Serializable workflow checkpoint independent of any graph library representation. */
public record WorkflowCheckpoint(
    UUID workflowId,
    long sequence,
    String nodeName,
    Map<String, Object> state,
    String nextNodeName,
    WorkflowStatus status,
    Instant createdAt) {

  public WorkflowCheckpoint {
    workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
    if (sequence < 0) {
      throw new IllegalArgumentException("sequence must be non-negative");
    }
    nodeName = requireText(nodeName, "nodeName");
    Objects.requireNonNull(state, "state must not be null");
    state = Map.copyOf(state);
    if (nextNodeName != null && nextNodeName.isBlank()) {
      throw new IllegalArgumentException("nextNodeName must not be blank when present");
    }
    status = Objects.requireNonNull(status, "status must not be null");
    createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field + " must not be null");
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }
}
