package com.emme.assistant.ai.application.guardrail;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.contracts.guardrail.ContextRequest;
import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.kernel.context.AiExecutionContext;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultContextGuardTest {

  private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");
  private static final AiExecutionContext CONTEXT = context();
  private final DefaultContextGuard guard =
      new DefaultContextGuard(Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void deniesARequestForAnotherTenant() {
    var request =
        request(UUID.randomUUID(), CONTEXT.principalId(), CONTEXT.roles(), CONTEXT.traceId());

    var decision = guard.check(request, CONTEXT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.DENY);
    assertThat(decision.code()).isEqualTo("context.tenant_mismatch");
  }

  @Test
  void deniesARequestForAnotherPrincipal() {
    var request =
        request(CONTEXT.tenantId(), UUID.randomUUID(), CONTEXT.roles(), CONTEXT.traceId());

    var decision = guard.check(request, CONTEXT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.DENY);
    assertThat(decision.code()).isEqualTo("context.principal_mismatch");
  }

  @Test
  void blocksAnExpiredDeadline() {
    var request =
        new ContextRequest(
            CONTEXT.tenantId(),
            CONTEXT.principalId(),
            CONTEXT.roles(),
            CONTEXT.traceId(),
            NOW.minusSeconds(1));

    var decision = guard.check(request, CONTEXT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.BLOCK);
    assertThat(decision.code()).isEqualTo("context.deadline_expired");
  }

  @Test
  void allowsAContextThatMatchesTrustedIdentity() {
    var decision =
        guard.check(
            request(CONTEXT.tenantId(), CONTEXT.principalId(), CONTEXT.roles(), CONTEXT.traceId()),
            CONTEXT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.ALLOW);
    assertThat(decision.code()).isEqualTo("context.allowed");
  }

  private static ContextRequest request(
      UUID tenantId, UUID principalId, Set<String> roles, String traceId) {
    return new ContextRequest(tenantId, principalId, roles, traceId, NOW.plusSeconds(30));
  }

  private static AiExecutionContext context() {
    return new AiExecutionContext(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Set.of("tenant_client"),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "trace-1",
        "idempotency-1");
  }
}
