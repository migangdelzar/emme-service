package com.emme.assistant.ai.adapter.out.provider.springai.advisor;

import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.ai.contracts.guardrail.OutputRequest;
import com.emme.assistant.ai.application.guardrail.GuardrailRejectedException;
import com.emme.assistant.ai.application.guardrail.OutputGuard;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Locale;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;

/** Enforces the typed output boundary after a Spring AI model call. */
public final class OutputGuardAdvisor implements BaseAdvisor {

  private final OutputGuard guard;

  public OutputGuardAdvisor(OutputGuard guard) {
    this.guard = Objects.requireNonNull(guard, "guard must not be null");
  }

  @Override
  public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
    return request;
  }

  @Override
  public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
    Objects.requireNonNull(response, "response must not be null");
    AiExecutionContext context = AiExecutionContextScope.requireCurrent();
    String text = response.chatResponse().getResult().getOutput().getText();
    GuardrailDecision decision =
        guard.check(
            new OutputRequest(
                context.channel().name().toLowerCase(Locale.ROOT), text, false, false),
            context);
    if (decision.action() != GuardrailAction.ALLOW) {
      throw new GuardrailRejectedException(decision);
    }
    return response;
  }

  @Override
  public String getName() {
    return "emme-output-guard";
  }

  @Override
  public int getOrder() {
    return 100;
  }
}
