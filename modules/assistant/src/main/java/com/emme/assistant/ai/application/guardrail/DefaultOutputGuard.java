package com.emme.assistant.ai.application.guardrail;

import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.ai.contracts.guardrail.OutputRequest;
import com.emme.kernel.context.AiExecutionContext;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Applies deterministic channel, structure, and sensitive-data checks to model output. */
public final class DefaultOutputGuard implements OutputGuard {

  private final Set<String> allowedChannels;

  public DefaultOutputGuard(Set<String> allowedChannels) {
    Objects.requireNonNull(allowedChannels, "allowedChannels must not be null");
    if (allowedChannels.stream().anyMatch(channel -> channel == null || channel.isBlank())) {
      throw new IllegalArgumentException("allowedChannels must not contain blank values");
    }
    this.allowedChannels = Set.copyOf(allowedChannels);
  }

  @Override
  public GuardrailDecision check(OutputRequest request, AiExecutionContext context) {
    Objects.requireNonNull(request, "request must not be null");
    Objects.requireNonNull(context, "context must not be null");
    if (!allowedChannels.contains(request.channel())) {
      return decision(GuardrailAction.DENY, "output.channel_not_allowed");
    }
    if (request.content().isBlank()) {
      return decision(GuardrailAction.BLOCK, "output.empty");
    }
    if (containsSensitiveData(request.content())) {
      return decision(GuardrailAction.BLOCK, "output.sensitive_data");
    }
    if (request.containsBusinessClaim() && !request.structured()) {
      return decision(GuardrailAction.REGENERATE, "output.structured_required");
    }
    return decision(GuardrailAction.ALLOW, "output.allowed");
  }

  private static boolean containsSensitiveData(String content) {
    String normalized = content.toLowerCase(Locale.ROOT);
    return normalized.matches(".*\\b(?:bearer\\s+)?[a-z0-9._-]{20,}\\b.*")
        || normalized.matches(".*\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b.*")
        || normalized.matches(".*\\b[\\w.+-]+@[\\w.-]+\\.[a-z]{2,}\\b.*")
        || normalized.matches(".*(?:\\+?\\d[\\d ()-]{8,}\\d).*");
  }

  private static GuardrailDecision decision(GuardrailAction action, String code) {
    return new GuardrailDecision(action, code, java.util.Map.of());
  }
}
