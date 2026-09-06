package com.emme.assistant.ai.application.guardrail;

import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.ai.contracts.guardrail.GuardrailRequest;
import com.emme.kernel.context.AiExecutionContext;
import java.util.Objects;

/** Ordered composition of the typed guardrail boundaries. */
public final class DefaultGuardrailPipeline implements GuardrailPipeline {

  private final InputGuard input;
  private final ContextGuard context;
  private final ToolGuard tool;
  private final GroundingGuard grounding;
  private final OutputGuard output;
  private final DeliveryGuard delivery;

  public DefaultGuardrailPipeline(
      InputGuard input,
      ContextGuard context,
      ToolGuard tool,
      GroundingGuard grounding,
      OutputGuard output,
      DeliveryGuard delivery) {
    this.input = Objects.requireNonNull(input, "input must not be null");
    this.context = Objects.requireNonNull(context, "context must not be null");
    this.tool = Objects.requireNonNull(tool, "tool must not be null");
    this.grounding = Objects.requireNonNull(grounding, "grounding must not be null");
    this.output = Objects.requireNonNull(output, "output must not be null");
    this.delivery = Objects.requireNonNull(delivery, "delivery must not be null");
  }

  @Override
  public GuardrailDecision evaluate(GuardrailRequest request, AiExecutionContext context) {
    Objects.requireNonNull(request, "request must not be null");
    Objects.requireNonNull(context, "context must not be null");
    GuardrailDecision decision = input.check(request.input(), context);
    if (stopsPipeline(decision)) return decision;
    decision = this.context.check(request.context(), context);
    if (stopsPipeline(decision)) return decision;
    decision = tool.check(request.tool(), context);
    if (stopsPipeline(decision)) return decision;
    decision = grounding.check(request.grounding(), context);
    if (stopsPipeline(decision)) return decision;
    decision = output.check(request.output(), context);
    if (stopsPipeline(decision)) return decision;
    return delivery.check(request.delivery(), context);
  }

  private static boolean stopsPipeline(GuardrailDecision decision) {
    Objects.requireNonNull(decision, "guardrail decision must not be null");
    return switch (decision.action()) {
      case BLOCK, DENY, ESCALATE, NO_ANSWER -> true;
      default -> false;
    };
  }
}
