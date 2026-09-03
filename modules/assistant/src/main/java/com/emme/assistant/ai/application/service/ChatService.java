package com.emme.assistant.ai.application.service;

import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.IdentifiedChatCompletionPort;
import com.emme.assistant.ai.application.port.out.NoopSemanticMetrics;
import com.emme.assistant.ai.application.port.out.ProactiveToolRouter;
import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticResponseCache;
import com.emme.assistant.ai.application.semantic.SemanticFailurePolicy;
import com.emme.assistant.ai.application.tool.AiToolResult;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Executes the chat capability through the configured model-provider boundary. */
@Service
public class ChatService implements ChatUseCase {
  private final ChatCompletionPort chatCompletion;
  private final Optional<SemanticResponseCache> semanticCache;
  private final Optional<ProactiveToolRouter> proactiveToolRouter;
  private final SemanticMetrics metrics;

  public ChatService(
      ChatCompletionPort chatCompletion, Optional<SemanticResponseCache> semanticCache) {
    this(
        chatCompletion,
        semanticCache,
        Optional.empty(),
        NoopSemanticMetrics.INSTANCE);
  }

  public ChatService(
      ChatCompletionPort chatCompletion,
      Optional<SemanticResponseCache> semanticCache,
      Optional<ProactiveToolRouter> proactiveToolRouter) {
    this(
        chatCompletion,
        semanticCache,
        proactiveToolRouter,
        NoopSemanticMetrics.INSTANCE);
  }

  @Autowired
  public ChatService(
      ChatCompletionPort chatCompletion,
      Optional<SemanticResponseCache> semanticCache,
      Optional<ProactiveToolRouter> proactiveToolRouter,
      SemanticMetrics metrics) {
    this.chatCompletion =
        Objects.requireNonNull(chatCompletion, "chatCompletion must not be null");
    this.semanticCache =
        Objects.requireNonNull(semanticCache, "semanticCache must not be null");
    this.proactiveToolRouter =
        Objects.requireNonNull(proactiveToolRouter, "proactiveToolRouter must not be null");
    this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
  }

  @Override
  public String chat(String conversationContext, String userMessage) {
    AiExecutionContextScope.requireCurrent();
    Optional<AiToolResult> proactiveToolResult;
    try {
      proactiveToolResult = proactiveToolRouter.flatMap(router -> router.route(userMessage));
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      recordFallback("semantic_tool_failure");
      proactiveToolResult = Optional.empty();
    }
    if (proactiveToolResult.isPresent()) {
      return proactiveToolResult.orElseThrow().content();
    }
    Optional<String> cached;
    try {
      cached = semanticCache.flatMap(cache -> cache.lookup(conversationContext, userMessage));
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      recordFallback("semantic_cache_failure");
      cached = Optional.empty();
    }
    if (cached.isPresent()) {
      return cached.orElseThrow();
    }
    Completion response;
    response = complete(chatCompletion, conversationContext, userMessage);
    Completion completed = response;
    String completedResponse = completed.content();
    try {
      semanticCache.ifPresent(
          cache -> {
            if (completed.identified()) {
              cache.store(
                  conversationContext,
                  userMessage,
                  completedResponse,
                  new com.emme.assistant.ai.application.semantic.SemanticCacheIdentity(
                      completed.provider(),
                      completed.model(),
                      "knowledge-v1",
                      "policy-v1",
                      "source-v1"));
            } else {
              cache.store(conversationContext, userMessage, completedResponse);
            }
          });
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      recordFallback("semantic_cache_write_failure");
      // Semantic caching is an optimization and must not make chat unavailable.
    }
    return completedResponse;
  }

  private static Completion complete(
      ChatCompletionPort chat, String conversationContext, String userMessage) {
    if (chat instanceof IdentifiedChatCompletionPort identified) {
      var result = identified.completeWithIdentity(conversationContext, userMessage);
      return new Completion(result.content(), result.provider(), result.model(), true);
    }
    return new Completion(
        chat.complete(conversationContext, userMessage), "legacy-provider", "legacy-model");
  }

  private record Completion(String content, String provider, String model, boolean identified) {
    private Completion(String content, String provider, String model) {
      this(content, provider, model, false);
    }
  }

  private void recordFallback(String reason) {
    try {
      metrics.recordFallback("chat", reason);
    } catch (RuntimeException ignored) {
      // Observability must not change chat semantics.
    }
  }
}
