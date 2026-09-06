package com.emme.assistant.ai.application.guardrail;

import com.emme.ai.contracts.guardrail.GroundingRequest;
import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.kernel.context.AiExecutionContext;

public interface GroundingGuard {
  GuardrailDecision check(GroundingRequest request, AiExecutionContext context);
}
