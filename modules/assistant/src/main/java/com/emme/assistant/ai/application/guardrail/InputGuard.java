package com.emme.assistant.ai.application.guardrail;

import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.ai.contracts.guardrail.InputRequest;
import com.emme.kernel.context.AiExecutionContext;

public interface InputGuard {
  GuardrailDecision check(InputRequest request, AiExecutionContext context);
}
