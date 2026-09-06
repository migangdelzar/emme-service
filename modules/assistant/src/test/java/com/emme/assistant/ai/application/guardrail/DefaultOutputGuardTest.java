package com.emme.assistant.ai.application.guardrail;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.ai.contracts.guardrail.OutputRequest;
import com.emme.kernel.context.AiExecutionContext;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultOutputGuardTest {

  private static final AiExecutionContext CONTEXT =
      new AiExecutionContext(
          UUID.randomUUID(),
          UUID.randomUUID(),
          Set.of("tenant_client"),
          UUID.randomUUID(),
          UUID.randomUUID(),
          "trace-1",
          "idempotency-1");
  private final DefaultOutputGuard guard = new DefaultOutputGuard(Set.of("web", "whatsapp"));

  @Test
  void deniesAnUnrecognizedDeliveryChannel() {
    var decision = guard.check(new OutputRequest("email", "hello", false, false), CONTEXT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.DENY);
    assertThat(decision.code()).isEqualTo("output.channel_not_allowed");
  }

  @Test
  void blocksEmptyOutput() {
    var decision = guard.check(new OutputRequest("web", " ", false, false), CONTEXT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.BLOCK);
    assertThat(decision.code()).isEqualTo("output.empty");
  }

  @Test
  void requestsStructuredOutputForAnUnstructuredBusinessClaim() {
    var decision = guard.check(new OutputRequest("web", "The price is 100", false, true), CONTEXT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.REGENERATE);
    assertThat(decision.code()).isEqualTo("output.structured_required");
  }

  @Test
  void blocksSensitiveData() {
    var decision =
        guard.check(
            new OutputRequest("web", "Use bearer abcdefghijklmnopqrst", false, false), CONTEXT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.BLOCK);
    assertThat(decision.code()).isEqualTo("output.sensitive_data");
  }

  @Test
  void allowsSafeOutput() {
    var decision =
        guard.check(new OutputRequest("web", "We are open today.", false, false), CONTEXT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.ALLOW);
    assertThat(decision.code()).isEqualTo("output.allowed");
  }
}
