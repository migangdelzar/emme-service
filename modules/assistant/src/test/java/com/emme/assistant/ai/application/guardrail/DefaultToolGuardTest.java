package com.emme.assistant.ai.application.guardrail;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.ai.contracts.guardrail.ToolRequest;
import com.emme.kernel.context.AiExecutionContext;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultToolGuardTest {

  private static final AiExecutionContext CLIENT = context(Set.of("tenant_client"));
  private static final AiExecutionContext STAFF = context(Set.of("tenant_staff"));
  private final DefaultToolGuard guard =
      new DefaultToolGuard(Set.of("faq.read", "appointment.cancel"), Set.of("appointment.cancel"));

  @Test
  void deniesToolsOutsideTheBackendAllowList() {
    var decision = guard.check(tool("admin.delete", false, false, CLIENT), CLIENT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.DENY);
    assertThat(decision.code()).isEqualTo("tool.not_allowed");
  }

  @Test
  void requiresStaffForStaffOnlyTools() {
    var decision = guard.check(tool("appointment.cancel", true, true, CLIENT), CLIENT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.DENY);
    assertThat(decision.code()).isEqualTo("tool.staff_required");
  }

  @Test
  void requiresConfirmationBeforeMutation() {
    var decision = guard.check(tool("appointment.cancel", true, false, STAFF), STAFF);

    assertThat(decision.action()).isEqualTo(GuardrailAction.CLARIFY);
    assertThat(decision.code()).isEqualTo("tool.confirmation_required");
  }

  @Test
  void rejectsAnIdempotencyMismatch() {
    var decision =
        guard.check(new ToolRequest("faq.read", Map.of(), false, false, "other-key"), CLIENT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.DENY);
    assertThat(decision.code()).isEqualTo("tool.idempotency_mismatch");
  }

  @Test
  void allowsAnAuthorizedReadOnlyTool() {
    var decision = guard.check(tool("faq.read", false, false, CLIENT), CLIENT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.ALLOW);
    assertThat(decision.code()).isEqualTo("tool.allowed");
  }

  private static ToolRequest tool(
      String key, boolean mutating, boolean confirmed, AiExecutionContext context) {
    return new ToolRequest(key, Map.of(), mutating, confirmed, context.idempotencyKey());
  }

  private static AiExecutionContext context(Set<String> roles) {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        roles,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-1",
        "idempotency-1");
  }
}
