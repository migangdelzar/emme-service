package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.ai.application.port.out.ModelProvider;
import com.emme.assistant.ai.application.port.out.SemanticResponseCache;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Executes the chat capability through the configured model-provider boundary. */
@Service
public class ChatService implements ChatUseCase {
  private final ModelProvider provider;
  private final Optional<SemanticResponseCache> semanticCache;

  public ChatService(ModelProvider provider, Optional<SemanticResponseCache> semanticCache) {
    this.provider = Objects.requireNonNull(provider, "provider must not be null");
    this.semanticCache = Objects.requireNonNull(semanticCache, "semanticCache must not be null");
  }

  @Override
  public String chat(String conversationContext, String userMessage) {
    Optional<String> cached =
        semanticCache.flatMap(cache -> cache.lookup(conversationContext, userMessage));
    if (cached.isPresent()) {
      return cached.orElseThrow();
    }
    String response = provider.chat(conversationContext, userMessage);
    semanticCache.ifPresent(cache -> cache.store(conversationContext, userMessage, response));
    return response;
  }
}
