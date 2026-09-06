package com.emme.assistant.ai.application.guardrail;

import com.emme.ai.contracts.guardrail.DeliveryRequest;
import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.kernel.context.AiExecutionContext;

public interface DeliveryGuard {
  GuardrailDecision check(DeliveryRequest request, AiExecutionContext context);
}
