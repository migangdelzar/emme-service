package com.emme.assistant.ai.application.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NodePolicyRegistryTest {

  @Test
  void rejectsDuplicateAndUnknownNodeIds() {
    NodeProfile profile = profile("answer");

    assertThatThrownBy(() -> new NodePolicyRegistry(List.of(profile, profile)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Duplicate node policy: answer");

    NodePolicyRegistry registry = new NodePolicyRegistry(List.of(profile));

    assertThat(registry.profile("answer")).isEqualTo(profile);
    assertThatThrownBy(() -> registry.profile("missing"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown workflow node: missing");
  }

  private static NodeProfile profile(String nodeId) {
    return new NodeProfile(
        nodeId,
        NodeModelRole.ANSWER,
        new NodeToolPolicy(Set.of("faq.read"), true, false),
        new NodeMemoryPolicy(Set.of("conversation"), 4, false),
        new NodeGuardrailPolicy(true, true, true, true, true),
        2,
        Duration.ofSeconds(5),
        false,
        false);
  }
}
