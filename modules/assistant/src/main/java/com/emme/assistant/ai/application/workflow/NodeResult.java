package com.emme.assistant.ai.application.workflow;

import com.emme.ai.contracts.guardrail.GuardrailDecision;
import java.util.Objects;

/** Typed result from one workflow node before adapter serialization. */
public record NodeResult<P>(P statePatch, GuardrailDecision decision) {

  public NodeResult {
    Objects.requireNonNull(statePatch, "statePatch must not be null");
    Objects.requireNonNull(decision, "decision must not be null");
  }
}
