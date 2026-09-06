package com.emme.assistant.ai.application.workflow;

import com.emme.kernel.context.AiExecutionContext;
import java.util.Objects;

/** Typed, policy-bound input visible to one workflow node invocation. */
public record NodeContext<S>(
    S visibleState, AiExecutionContext executionContext, NodeProfile profile) {

  public NodeContext {
    Objects.requireNonNull(visibleState, "visibleState must not be null");
    Objects.requireNonNull(executionContext, "executionContext must not be null");
    Objects.requireNonNull(profile, "profile must not be null");
  }
}
