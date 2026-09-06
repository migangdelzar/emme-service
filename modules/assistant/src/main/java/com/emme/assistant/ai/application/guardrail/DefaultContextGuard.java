package com.emme.assistant.ai.application.guardrail;

import com.emme.ai.contracts.guardrail.ContextRequest;
import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.kernel.context.AiExecutionContext;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Verifies that a guardrail request still matches the trusted backend context. */
public final class DefaultContextGuard implements ContextGuard {

  private final Clock clock;

  public DefaultContextGuard(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public GuardrailDecision check(ContextRequest request, AiExecutionContext context) {
    Objects.requireNonNull(request, "request must not be null");
    Objects.requireNonNull(context, "context must not be null");
    if (!request.tenantId().equals(context.tenantId())) {
      return decision(GuardrailAction.DENY, "context.tenant_mismatch");
    }
    if (!request.principalId().equals(context.principalId())) {
      return decision(GuardrailAction.DENY, "context.principal_mismatch");
    }
    if (!request.roles().equals(context.roles())) {
      return decision(GuardrailAction.DENY, "context.roles_mismatch");
    }
    if (!request.traceId().equals(context.traceId())) {
      return decision(GuardrailAction.DENY, "context.trace_mismatch");
    }
    if (!request.deadline().isAfter(Instant.now(clock))) {
      return decision(GuardrailAction.BLOCK, "context.deadline_expired");
    }
    return decision(GuardrailAction.ALLOW, "context.allowed");
  }

  private static GuardrailDecision decision(GuardrailAction action, String code) {
    return new GuardrailDecision(action, code, java.util.Map.of());
  }
}
