package com.emme.assistant.ai.adapter.out.provider.springai.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.assistant.ai.application.guardrail.GuardrailRejectedException;
import com.emme.assistant.ai.application.guardrail.OutputGuard;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

class OutputGuardAdvisorTest {

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
  void rejectsAnUnsafeModelResponse() {
    OutputGuard guard = mock(OutputGuard.class);
    when(guard.check(any(), same(CONTEXT)))
        .thenReturn(new GuardrailDecision(GuardrailAction.BLOCK, "output.unsafe", Map.of()));
    OutputGuardAdvisor advisor = new OutputGuardAdvisor(guard);

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    CONTEXT, () -> advisor.after(response("unsafe"), null)))
        .isInstanceOf(GuardrailRejectedException.class)
        .hasMessage("AI output rejected by guardrail: output.unsafe");
  }

  @Test
  void passesAnAllowedModelResponseThrough() {
    OutputGuard guard = mock(OutputGuard.class);
    when(guard.check(any(), same(CONTEXT)))
        .thenReturn(new GuardrailDecision(GuardrailAction.ALLOW, "output.allowed", Map.of()));
    OutputGuardAdvisor advisor = new OutputGuardAdvisor(guard);
    ChatClientResponse response = response("safe");

    var result = AiExecutionContextScope.call(CONTEXT, () -> advisor.after(response, null));

    assertThat(result).isSameAs(response);
  }

  private static ChatClientResponse response(String text) {
    return new ChatClientResponse(
        new ChatResponse(List.of(new Generation(new AssistantMessage(text)))), Map.of());
  }
}
