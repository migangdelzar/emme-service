package com.emme.assistant.ai.application.guardrail;

import com.emme.ai.contracts.guardrail.ContextRequest;
import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.kernel.context.AiExecutionContext;

public interface ContextGuard {
  GuardrailDecision check(ContextRequest request, AiExecutionContext context);
}
