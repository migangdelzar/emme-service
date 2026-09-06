package com.emme.ai.platform.adapter.out.provider.mock;

import com.emme.ai.contracts.image.CaptionImageUseCase;
import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.ChatResponse;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Always-available mock provider with echo chat and deterministic embeddings. Used when no real
 * provider (Ollama/OpenAI/Groq) is configured or available. Activated when app.ai.provider is
 * "mock" or not set at all.
 */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockModelProvider implements AiChatCompletion, CaptionImageUseCase {

  private String chat(String context, String userMessage) {
    AiExecutionContextScope.requireCurrent();
    return "MOCK: I received your message: \""
        + userMessage
        + "\". "
        + "Configure a real AI provider (Ollama/OpenAI) for intelligent responses.";
  }

  @Override
  public ChatResponse complete(Request request) {
    AiExecutionContextScope.requireCurrent();
    if (!request.providerPolicy().admittedProviders().contains("mock")) {
      throw new IllegalArgumentException("chat provider is not admitted by the request policy");
    }
    return new ChatResponse(
        chat(request.conversationContext(), request.userMessage()), "mock", "mock-v1", 0, 0);
  }

  @Override
  public String caption(String imageBase64) {
    return "maqueta de imagen " + UUID.randomUUID().toString().substring(0, 8);
  }
}
