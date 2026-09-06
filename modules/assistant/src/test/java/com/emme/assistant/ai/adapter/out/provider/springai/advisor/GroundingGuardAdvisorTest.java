package com.emme.assistant.ai.adapter.out.provider.springai.advisor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.assistant.ai.application.guardrail.GroundingGuard;
import com.emme.assistant.ai.application.guardrail.GuardrailRejectedException;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;

class GroundingGuardAdvisorTest {

  private static final AiExecutionContext CONTEXT =
      new AiExecutionContext(
          UUID.randomUUID(),
          UUID.randomUUID(),
          Set.of("tenant_client"),
          UUID.randomUUID(),
          UUID.randomUUID(),
          "trace-1",
          "idempotency-1");

  @Test
  void rejectsARequestWhenRetrievedProvenanceIsRejected() {
    GroundingGuard guard = mock(GroundingGuard.class);
    when(guard.check(any(), same(CONTEXT)))
        .thenReturn(
            new GuardrailDecision(GuardrailAction.NO_ANSWER, "grounding.rejected", Map.of()));
    GroundingGuardAdvisor advisor = new GroundingGuardAdvisor(guard);
    ChatClientRequest request =
        new ChatClientRequest(
            new Prompt("hello"),
            Map.of(
                RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT,
                List.of(new Document("faq-a", Map.of("score", 0.91)))));

    assertThatThrownBy(
            () -> AiExecutionContextScope.call(CONTEXT, () -> advisor.before(request, null)))
        .isInstanceOf(GuardrailRejectedException.class)
        .hasMessage("AI grounding rejected by guardrail: grounding.rejected");
  }
}
