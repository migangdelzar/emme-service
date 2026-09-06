package com.emme.assistant.ai.application.guardrail;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.contracts.guardrail.DeliveryRequest;
import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.kernel.context.AiExecutionContext;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultDeliveryGuardTest {

  private static final AiExecutionContext CONTEXT =
      new AiExecutionContext(
          UUID.randomUUID(),
          UUID.randomUUID(),
          Set.of("tenant_client"),
          UUID.randomUUID(),
          UUID.randomUUID(),
          "trace-1",
          "idempotency-1");
  private final DefaultDeliveryGuard guard = new DefaultDeliveryGuard(Set.of("web", "whatsapp"));

  @Test
  void deniesAnUnsupportedChannel() {
    var decision = guard.check(new DeliveryRequest("email", "hello", 100, false), CONTEXT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.DENY);
    assertThat(decision.code()).isEqualTo("delivery.channel_not_allowed");
  }

  @Test
  void blocksContentOverTheChannelLimit() {
    var decision = guard.check(new DeliveryRequest("web", "hello", 4, false), CONTEXT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.BLOCK);
    assertThat(decision.code()).isEqualTo("delivery.too_long");
  }

  @Test
  void blocksBlankContent() {
    var decision = guard.check(new DeliveryRequest("web", " ", 100, false), CONTEXT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.BLOCK);
    assertThat(decision.code()).isEqualTo("delivery.empty");
  }

  @Test
  void deliversSafeStreamingContent() {
    var decision = guard.check(new DeliveryRequest("whatsapp", "hello", 100, true), CONTEXT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.DELIVER);
    assertThat(decision.code()).isEqualTo("delivery.allowed");
  }
}
