package com.emme.assistant.ai.application.guardrail;

import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.ai.contracts.guardrail.GuardrailRequest;
import com.emme.kernel.context.AiExecutionContext;

public interface GuardrailPipeline {
  GuardrailDecision evaluate(GuardrailRequest request, AiExecutionContext context);
}
