package com.emme.ai.contracts.workflow;

import java.util.Objects;
import java.util.UUID;

/** Stable workflow reference returned to API and channel adapters. */
public record WorkflowHandle(UUID workflowId, WorkflowStatus status, long version) {

  public WorkflowHandle {
    workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
    status = Objects.requireNonNull(status, "status must not be null");
    if (version < 0) {
      throw new IllegalArgumentException("version must be non-negative");
    }
  }
}
