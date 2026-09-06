package com.emme.ai.platform.adapter.out.provider.springai;

import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.ChatResponse;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;

/** Adapts a provider-identified Spring AI chat model to the canonical chat capability. */
public final class SpringAiChatCompletion implements AiChatCompletion {

  private final SpringAiChatModel model;

  public SpringAiChatCompletion(SpringAiChatModel model) {
    this.model = Objects.requireNonNull(model, "model must not be null");
  }

  @Override
  public ChatResponse complete(Request request) {
    Objects.requireNonNull(request, "request must not be null");
    if (!AiExecutionContextScope.requireCurrent().equals(request.executionContext())) {
      throw new IllegalArgumentException(
          "chat request context must match the bound AI execution context");
    }
    if (!request.providerPolicy().admittedProviders().contains(model.provider())) {
      throw new IllegalArgumentException(
          "chat provider is not admitted by the request policy: " + model.provider());
    }
    return new ChatResponse(
        model.complete(request.conversationContext(), request.userMessage()),
        model.provider(),
        model.modelVersion(),
        0,
        0);
  }
}
