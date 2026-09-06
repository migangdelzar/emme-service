package com.emme.assistant.ai.application.guardrail;

import com.emme.ai.contracts.guardrail.GuardrailDecision;
import java.util.Objects;

/** Signals a deterministic guardrail rejection without converting it to provider failover. */
public final class GuardrailRejectedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final transient GuardrailDecision decision;

  public GuardrailRejectedException(GuardrailDecision decision) {
    super(message(Objects.requireNonNull(decision, "decision must not be null")));
    this.decision = decision;
  }

  public GuardrailDecision decision() {
    return decision;
  }

  private static String message(GuardrailDecision decision) {
    return "AI "
        + decision.code().substring(0, decision.code().indexOf('.'))
        + " rejected by guardrail: "
        + decision.code();
  }
}
