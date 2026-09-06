package com.emme.assistant.ai.application.workflow;

import java.time.Duration;
import java.util.Objects;

/** Complete, immutable policy for one graph node. */
public record NodeProfile(
    String nodeId,
    NodeModelRole modelRole,
    NodeToolPolicy tools,
    NodeMemoryPolicy memory,
    NodeGuardrailPolicy guardrails,
    int maxToolCalls,
    Duration timeout,
    boolean mayInterrupt,
    boolean requiresApproval) {

  public NodeProfile {
    Objects.requireNonNull(nodeId, "nodeId must not be null");
    if (nodeId.isBlank()) {
      throw new IllegalArgumentException("nodeId must not be blank");
    }
    Objects.requireNonNull(modelRole, "modelRole must not be null");
    Objects.requireNonNull(tools, "tools must not be null");
    Objects.requireNonNull(memory, "memory must not be null");
    Objects.requireNonNull(guardrails, "guardrails must not be null");
    if (maxToolCalls < 0) {
      throw new IllegalArgumentException("maxToolCalls must not be negative");
    }
    Objects.requireNonNull(timeout, "timeout must not be null");
    if (timeout.isZero() || timeout.isNegative()) {
      throw new IllegalArgumentException("timeout must be positive");
    }
  }
}
