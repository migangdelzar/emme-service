package com.emme.assistant.ai.application.guardrail;

import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.ai.contracts.guardrail.ToolRequest;
import com.emme.assistant.ai.application.security.AiStaffRolePolicy;
import com.emme.kernel.context.AiExecutionContext;
import java.util.Objects;
import java.util.Set;

/** Applies backend authorization and confirmation policy before a tool can execute. */
public final class DefaultToolGuard implements ToolGuard {

  private final Set<String> allowedToolKeys;
  private final Set<String> staffOnlyToolKeys;

  public DefaultToolGuard(Set<String> allowedToolKeys, Set<String> staffOnlyToolKeys) {
    this.allowedToolKeys = copyKeys(allowedToolKeys, "allowedToolKeys");
    this.staffOnlyToolKeys = copyKeys(staffOnlyToolKeys, "staffOnlyToolKeys");
    if (!this.allowedToolKeys.containsAll(this.staffOnlyToolKeys)) {
      throw new IllegalArgumentException("staffOnlyToolKeys must be contained in allowedToolKeys");
    }
  }

  @Override
  public GuardrailDecision check(ToolRequest request, AiExecutionContext context) {
    Objects.requireNonNull(request, "request must not be null");
    Objects.requireNonNull(context, "context must not be null");
    if (!allowedToolKeys.contains(request.toolKey())) {
      return decision(GuardrailAction.DENY, "tool.not_allowed");
    }
    if (!request.idempotencyKey().equals(context.idempotencyKey())) {
      return decision(GuardrailAction.DENY, "tool.idempotency_mismatch");
    }
    if (staffOnlyToolKeys.contains(request.toolKey())
        && !AiStaffRolePolicy.isStaff(context.roles())) {
      return decision(GuardrailAction.DENY, "tool.staff_required");
    }
    if (request.mutating() && !request.confirmed()) {
      return decision(GuardrailAction.CLARIFY, "tool.confirmation_required");
    }
    return decision(GuardrailAction.ALLOW, "tool.allowed");
  }

  private static Set<String> copyKeys(Set<String> values, String field) {
    Objects.requireNonNull(values, field + " must not be null");
    if (values.stream().anyMatch(value -> value == null || value.isBlank())) {
      throw new IllegalArgumentException(field + " must not contain blank values");
    }
    return Set.copyOf(values);
  }

  private static GuardrailDecision decision(GuardrailAction action, String code) {
    return new GuardrailDecision(action, code, java.util.Map.of());
  }
}
