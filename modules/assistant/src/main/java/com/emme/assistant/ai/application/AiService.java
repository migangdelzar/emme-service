package com.emme.assistant.ai.application;

import java.util.List;
import org.springframework.stereotype.Service;

@org.springframework.modulith.NamedInterface("ai-api")
@Service
public class AiService {

  private final ModelProvider provider;

  public AiService(ModelProvider provider) {
    this.provider = provider;
  }

  public String chat(String conversationContext, String userMessage) {
    return provider.chat(conversationContext, userMessage);
  }

  public List<Float> embed(String text) {
    return provider.embed(text);
  }

  public ModelProvider.IntentResult routeIntent(String message) {
    return provider.routeIntent(message);
  }

  public boolean isMockMode() {
    return provider.isMock();
  }

  public String providerName() {
    return provider.name();
  }
}
