package com.emme.assistant.ai.application.guardrail;

import com.emme.ai.contracts.guardrail.GroundingRequest;
import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.kernel.context.AiExecutionContext;
import java.util.Objects;

/** Prevents business answers from being generated without accepted source provenance. */
public final class DefaultGroundingGuard implements GroundingGuard {

  @Override
  public GuardrailDecision check(GroundingRequest request, AiExecutionContext context) {
    Objects.requireNonNull(request, "request must not be null");
    Objects.requireNonNull(context, "context must not be null");
    if (!request.retrievalAccepted()) {
      return decision(GuardrailAction.NO_ANSWER, "grounding.rejected");
    }
    if (request.sourceIds().isEmpty()) {
      return decision(GuardrailAction.NO_ANSWER, "grounding.sources_missing");
    }
    return decision(GuardrailAction.ALLOW, "grounding.accepted");
  }

  private static GuardrailDecision decision(GuardrailAction action, String code) {
    return new GuardrailDecision(action, code, java.util.Map.of());
  }
}
