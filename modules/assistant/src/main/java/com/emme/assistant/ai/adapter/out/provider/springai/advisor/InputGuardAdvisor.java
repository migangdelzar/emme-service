package com.emme.assistant.ai.adapter.out.provider.springai.advisor;

import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.ai.contracts.guardrail.InputRequest;
import com.emme.assistant.ai.application.guardrail.GuardrailRejectedException;
import com.emme.assistant.ai.application.guardrail.InputGuard;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;

/** Enforces the typed input boundary before a Spring AI model call. */
public final class InputGuardAdvisor implements BaseAdvisor {

  private final InputGuard guard;

  public InputGuardAdvisor(InputGuard guard) {
    this.guard = Objects.requireNonNull(guard, "guard must not be null");
  }

  @Override
  public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
    Objects.requireNonNull(request, "request must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    var userMessage = request.prompt().getUserMessage();
    String text = request.prompt().getContents();
    int attachmentCount = userMessage == null ? 0 : userMessage.getMedia().size();
    GuardrailDecision decision =
        guard.check(
            new InputRequest(
                text,
                text.getBytes(StandardCharsets.UTF_8).length,
                attachmentCount,
                context.idempotencyKey()),
            context);
    if (decision.action() != GuardrailAction.ALLOW) {
      throw new GuardrailRejectedException(decision);
    }
    return request;
  }

  @Override
  public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
    return response;
  }

  @Override
  public String getName() {
    return "emme-input-guard";
  }

  @Override
  public int getOrder() {
    return -200;
  }
}
