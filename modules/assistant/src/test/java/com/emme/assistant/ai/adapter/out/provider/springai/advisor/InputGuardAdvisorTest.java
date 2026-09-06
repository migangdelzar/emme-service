package com.emme.assistant.ai.adapter.out.provider.springai.advisor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.assistant.ai.application.guardrail.GuardrailRejectedException;
import com.emme.assistant.ai.application.guardrail.InputGuard;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.prompt.Prompt;

class InputGuardAdvisorTest {

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
  void rejectsARequestWhenTheInputGuardBlocksIt() {
    InputGuard guard = mock(InputGuard.class);
    when(guard.check(any(), same(CONTEXT)))
        .thenReturn(new GuardrailDecision(GuardrailAction.BLOCK, "input.blocked", Map.of()));
    InputGuardAdvisor advisor = new InputGuardAdvisor(guard);

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    CONTEXT,
                    () ->
                        advisor.before(new ChatClientRequest(new Prompt("hello"), Map.of()), null)))
        .isInstanceOf(GuardrailRejectedException.class)
        .hasMessage("AI input rejected by guardrail: input.blocked");
  }

  @Test
  void passesAnAllowedRequestThrough() {
    InputGuard guard = mock(InputGuard.class);
    when(guard.check(any(), same(CONTEXT)))
        .thenReturn(new GuardrailDecision(GuardrailAction.ALLOW, "input.allowed", Map.of()));
    InputGuardAdvisor advisor = new InputGuardAdvisor(guard);
    ChatClientRequest request = new ChatClientRequest(new Prompt("hello"), Map.of());

    var result = AiExecutionContextScope.call(CONTEXT, () -> advisor.before(request, null));

    org.assertj.core.api.Assertions.assertThat(result).isSameAs(request);
  }
}
