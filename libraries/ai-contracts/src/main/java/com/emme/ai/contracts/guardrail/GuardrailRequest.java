package com.emme.ai.contracts.guardrail;

import java.util.Objects;

/** Bounded request envelope passed through the ordered guardrail pipeline. */
public record GuardrailRequest(
    InputRequest input,
    ContextRequest context,
    ToolRequest tool,
    GroundingRequest grounding,
    OutputRequest output,
    DeliveryRequest delivery) {

  public GuardrailRequest {
    Objects.requireNonNull(input, "input must not be null");
    Objects.requireNonNull(context, "context must not be null");
    Objects.requireNonNull(tool, "tool must not be null");
    Objects.requireNonNull(grounding, "grounding must not be null");
    Objects.requireNonNull(output, "output must not be null");
    Objects.requireNonNull(delivery, "delivery must not be null");
  }
}
