package com.emme.ai.contracts.guardrail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class GuardrailContractTest {

  @Test
  void exposesTheTypedGuardrailActions() {
    assertThat(GuardrailAction.values())
        .containsExactly(
            GuardrailAction.ALLOW,
            GuardrailAction.REDACT,
            GuardrailAction.CLARIFY,
            GuardrailAction.DENY,
            GuardrailAction.BLOCK,
            GuardrailAction.ESCALATE,
            GuardrailAction.REGENERATE,
            GuardrailAction.NO_ANSWER,
            GuardrailAction.DELIVER);
  }

  @Test
  void requiresAReasonCodeAndKeepsSafeAttributesBoundedAndImmutable() {
    var attributes = Map.of("scope", "input");
    var decision = new GuardrailDecision(GuardrailAction.BLOCK, "input.invalid", attributes);

    assertThat(decision.safeAttributes()).isEqualTo(attributes);
    assertThatThrownBy(() -> decision.safeAttributes().put("secret", "must not be mutable"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> new GuardrailDecision(GuardrailAction.ALLOW, " ", Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("code must not be blank");
  }

  @Test
  void rejectsAnUnboundedSafeAttributeSet() {
    Map<String, String> attributes = new java.util.LinkedHashMap<>();
    for (int index = 0; index < 17; index++) {
      attributes.put("key-" + index, "value");
    }

    assertThatThrownBy(
            () -> new GuardrailDecision(GuardrailAction.ALLOW, "input.allowed", attributes))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("safeAttributes must contain at most 16 entries");
  }
}
