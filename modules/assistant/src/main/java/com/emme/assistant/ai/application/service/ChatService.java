package com.emme.assistant.ai.application.service;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import com.emme.assistant.ai.application.port.out.EmbeddingProviderUnavailableException;
import com.emme.assistant.ai.application.port.out.ProactiveToolRouter;
import com.emme.assistant.ai.application.port.out.SemanticResponseCache;
import com.emme.assistant.ai.application.tool.AiToolResult;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Executes the chat capability through the configured model-provider boundary. */
@Service
public class ChatService implements ChatUseCase {
  private final AiModelProvider provider;
  private final Optional<SemanticResponseCache> semanticCache;
  private final Optional<ChatCompletionPort> chatCompletion;
  private final Optional<ProactiveToolRouter> proactiveToolRouter;

  public ChatService(AiModelProvider provider, Optional<SemanticResponseCache> semanticCache) {
    this(provider, semanticCache, Optional.empty(), Optional.empty());
  }

  public ChatService(
      AiModelProvider provider,
      Optional<SemanticResponseCache> semanticCache,
      Optional<ChatCompletionPort> chatCompletion) {
    this(provider, semanticCache, chatCompletion, Optional.empty());
  }

  @Autowired
  public ChatService(
      AiModelProvider provider,
      Optional<SemanticResponseCache> semanticCache,
      Optional<ChatCompletionPort> chatCompletion,
      Optional<ProactiveToolRouter> proactiveToolRouter) {
    this.provider = Objects.requireNonNull(provider, "provider must not be null");
    this.semanticCache = Objects.requireNonNull(semanticCache, "semanticCache must not be null");
    this.chatCompletion = Objects.requireNonNull(chatCompletion, "chatCompletion must not be null");
    this.proactiveToolRouter =
        Objects.requireNonNull(proactiveToolRouter, "proactiveToolRouter must not be null");
  }

  @Override
  public String chat(String conversationContext, String userMessage) {
    Optional<AiToolResult> proactiveToolResult;
    try {
      proactiveToolResult = proactiveToolRouter.flatMap(router -> router.route(userMessage));
    } catch (EmbeddingProviderUnavailableException ignored) {
      proactiveToolResult = Optional.empty();
    }
    if (proactiveToolResult.isPresent()) {
      return proactiveToolResult.orElseThrow().content();
    }
    Optional<String> cached;
    try {
      cached = semanticCache.flatMap(cache -> cache.lookup(conversationContext, userMessage));
    } catch (EmbeddingProviderUnavailableException ignored) {
      cached = Optional.empty();
    }
    if (cached.isPresent()) {
      return cached.orElseThrow();
    }
    String response;
    try {
      response =
          chatCompletion
              .map(chat -> chat.complete(conversationContext, userMessage))
              .orElseGet(() -> provider.chat(conversationContext, userMessage));
    } catch (ChatProviderUnavailableException unavailable) {
      response = provider.chat(conversationContext, userMessage);
    }
    String completedResponse = response;
    try {
      semanticCache.ifPresent(
          cache -> cache.store(conversationContext, userMessage, completedResponse));
    } catch (EmbeddingProviderUnavailableException ignored) {
      // Semantic caching is an optimization and must not make chat unavailable.
    }
    return completedResponse;
  }
}
