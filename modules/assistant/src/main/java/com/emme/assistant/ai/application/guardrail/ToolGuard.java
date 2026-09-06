package com.emme.assistant.ai.application.guardrail;

import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.ai.contracts.guardrail.ToolRequest;
import com.emme.kernel.context.AiExecutionContext;

public interface ToolGuard {
  GuardrailDecision check(ToolRequest request, AiExecutionContext context);
}
