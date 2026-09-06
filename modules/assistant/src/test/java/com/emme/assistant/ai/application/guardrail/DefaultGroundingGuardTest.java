package com.emme.assistant.ai.application.guardrail;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.ai.contracts.guardrail.GroundingRequest;
import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.kernel.context.AiExecutionContext;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultGroundingGuardTest {

  private static final AiExecutionContext CONTEXT =
      new AiExecutionContext(
          UUID.randomUUID(),
          UUID.randomUUID(),
          Set.of("tenant_client"),
          UUID.randomUUID(),
          UUID.randomUUID(),
          "trace-1",
          "idempotency-1");
  private final DefaultGroundingGuard guard = new DefaultGroundingGuard();

  @Test
  void returnsNoAnswerWhenRetrievalWasRejected() {
    var decision = guard.check(new GroundingRequest(false, 0.9, 0.2, List.of()), CONTEXT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.NO_ANSWER);
    assertThat(decision.code()).isEqualTo("grounding.rejected");
  }

  @Test
  void returnsNoAnswerWhenAcceptedRetrievalHasNoSources() {
    var decision = guard.check(new GroundingRequest(true, 0.9, 0.2, List.of()), CONTEXT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.NO_ANSWER);
    assertThat(decision.code()).isEqualTo("grounding.sources_missing");
  }

  @Test
  void allowsAcceptedRetrievalWithBoundedProvenance() {
    var decision = guard.check(new GroundingRequest(true, 0.9, 0.2, List.of("doc-1")), CONTEXT);

    assertThat(decision.action()).isEqualTo(GuardrailAction.ALLOW);
    assertThat(decision.code()).isEqualTo("grounding.accepted");
  }
}
