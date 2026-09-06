package com.emme.assistant.ai.application.guardrail;

import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.ai.contracts.guardrail.InputRequest;
import com.emme.kernel.context.AiExecutionContext;
import java.util.Locale;
import java.util.Objects;

/** Deterministic boundary checks for inbound AI messages. */
public final class DefaultInputGuard implements InputGuard {

  private final long maximumContentBytes;
  private final int maximumAttachmentCount;

  public DefaultInputGuard(long maximumContentBytes, int maximumAttachmentCount) {
    if (maximumContentBytes <= 0) {
      throw new IllegalArgumentException("maximumContentBytes must be positive");
    }
    if (maximumAttachmentCount < 0) {
      throw new IllegalArgumentException("maximumAttachmentCount must not be negative");
    }
    this.maximumContentBytes = maximumContentBytes;
    this.maximumAttachmentCount = maximumAttachmentCount;
  }

  @Override
  public GuardrailDecision check(InputRequest request, AiExecutionContext context) {
    Objects.requireNonNull(request, "request must not be null");
    Objects.requireNonNull(context, "context must not be null");
    if (!request.idempotencyKey().equals(context.idempotencyKey())) {
      return decision(GuardrailAction.DENY, "input.idempotency_mismatch");
    }
    if (request.message().isBlank()) {
      return decision(GuardrailAction.CLARIFY, "input.blank");
    }
    if (request.contentBytes() > maximumContentBytes) {
      return decision(GuardrailAction.BLOCK, "input.too_large");
    }
    if (request.attachmentCount() > maximumAttachmentCount) {
      return decision(GuardrailAction.BLOCK, "input.attachments_exceeded");
    }
    if (containsPromptInjection(request.message())) {
      return decision(GuardrailAction.BLOCK, "input.prompt_injection");
    }
    return decision(GuardrailAction.ALLOW, "input.allowed");
  }

  private static boolean containsPromptInjection(String message) {
    String normalized = message.toLowerCase(Locale.ROOT);
    return normalized.contains("ignore previous instructions")
        || normalized.contains("ignore all previous instructions")
        || normalized.contains("reveal the system prompt")
        || normalized.contains("show the system prompt");
  }

  private static GuardrailDecision decision(GuardrailAction action, String code) {
    return new GuardrailDecision(action, code, java.util.Map.of());
  }
}
