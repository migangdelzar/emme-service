package com.emme.assistant.ai.application.guardrail;

import com.emme.ai.contracts.guardrail.DeliveryRequest;
import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.kernel.context.AiExecutionContext;
import java.util.Objects;
import java.util.Set;

/** Applies channel and length checks before a response is delivered. */
public final class DefaultDeliveryGuard implements DeliveryGuard {

  private final Set<String> allowedChannels;

  public DefaultDeliveryGuard(Set<String> allowedChannels) {
    Objects.requireNonNull(allowedChannels, "allowedChannels must not be null");
    if (allowedChannels.stream().anyMatch(channel -> channel == null || channel.isBlank())) {
      throw new IllegalArgumentException("allowedChannels must not contain blank values");
    }
    this.allowedChannels = Set.copyOf(allowedChannels);
  }

  @Override
  public GuardrailDecision check(DeliveryRequest request, AiExecutionContext context) {
    Objects.requireNonNull(request, "request must not be null");
    Objects.requireNonNull(context, "context must not be null");
    if (!allowedChannels.contains(request.channel())) {
      return decision(GuardrailAction.DENY, "delivery.channel_not_allowed");
    }
    if (request.content().isBlank()) {
      return decision(GuardrailAction.BLOCK, "delivery.empty");
    }
    if (request.content().length() > request.maximumCharacters()) {
      return decision(GuardrailAction.BLOCK, "delivery.too_long");
    }
    return decision(GuardrailAction.DELIVER, "delivery.allowed");
  }

  private static GuardrailDecision decision(GuardrailAction action, String code) {
    return new GuardrailDecision(action, code, java.util.Map.of());
  }
}
