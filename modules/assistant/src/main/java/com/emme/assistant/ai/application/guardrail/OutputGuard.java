package com.emme.assistant.ai.application.guardrail;

import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.ai.contracts.guardrail.OutputRequest;
import com.emme.kernel.context.AiExecutionContext;

public interface OutputGuard {
  GuardrailDecision check(OutputRequest request, AiExecutionContext context);
}
