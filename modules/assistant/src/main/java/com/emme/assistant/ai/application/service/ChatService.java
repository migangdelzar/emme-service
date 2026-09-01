package com.emme.assistant.ai.application.service;

import com.emme.ai.contracts.model.AiModelProvider;
import com.emme.ai.contracts.model.ModelCapability;
import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.ai.application.port.out.ChatCompletionPort;
import com.emme.assistant.ai.application.port.out.ChatProviderUnavailableException;
import com.emme.assistant.ai.application.port.out.NoopSemanticMetrics;
import com.emme.assistant.ai.application.port.out.ProactiveToolRouter;
import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticResponseCache;
import com.emme.assistant.ai.application.semantic.SemanticFailurePolicy;
import com.emme.assistant.ai.application.tool.AiToolResult;
import com.emme.assistant.ai.configuration.AiExecutorProperties;
import com.emme.kernel.context.AiExecutionContextScope;
import java.time.Duration;
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
  private final Optional<ModelExecutionScheduler> modelExecutionScheduler;
  private final Duration admissionTimeout;
  private final SemanticMetrics metrics;

  public ChatService(AiModelProvider provider, Optional<SemanticResponseCache> semanticCache) {
    this(
        provider,
        semanticCache,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Duration.ofSeconds(5),
        NoopSemanticMetrics.INSTANCE);
  }

  public ChatService(
      AiModelProvider provider,
      Optional<SemanticResponseCache> semanticCache,
      Optional<ChatCompletionPort> chatCompletion) {
    this(
        provider,
        semanticCache,
        chatCompletion,
        Optional.empty(),
        Optional.empty(),
        Duration.ofSeconds(5),
        NoopSemanticMetrics.INSTANCE);
  }

  public ChatService(
      AiModelProvider provider,
      Optional<SemanticResponseCache> semanticCache,
      Optional<ChatCompletionPort> chatCompletion,
      Optional<ProactiveToolRouter> proactiveToolRouter) {
    this(
        provider,
        semanticCache,
        chatCompletion,
        proactiveToolRouter,
        Optional.empty(),
        Duration.ofSeconds(5),
        NoopSemanticMetrics.INSTANCE);
  }

  @Autowired
  public ChatService(
      AiModelProvider provider,
      Optional<SemanticResponseCache> semanticCache,
      Optional<ChatCompletionPort> chatCompletion,
      Optional<ProactiveToolRouter> proactiveToolRouter,
      Optional<ModelExecutionScheduler> modelExecutionScheduler,
      AiExecutorProperties executionProperties,
      SemanticMetrics metrics) {
    this(
        provider,
        semanticCache,
        chatCompletion,
        proactiveToolRouter,
        modelExecutionScheduler,
        executionProperties.modelAdmissionTimeout(),
        metrics);
  }

  public ChatService(
      AiModelProvider provider,
      Optional<SemanticResponseCache> semanticCache,
      Optional<ChatCompletionPort> chatCompletion,
      Optional<ProactiveToolRouter> proactiveToolRouter,
      Optional<ModelExecutionScheduler> modelExecutionScheduler,
      Duration admissionTimeout) {
    this(
        provider,
        semanticCache,
        chatCompletion,
        proactiveToolRouter,
        modelExecutionScheduler,
        admissionTimeout,
        NoopSemanticMetrics.INSTANCE);
  }

  public ChatService(
      AiModelProvider provider,
      Optional<SemanticResponseCache> semanticCache,
      Optional<ChatCompletionPort> chatCompletion,
      Optional<ProactiveToolRouter> proactiveToolRouter,
      Optional<ModelExecutionScheduler> modelExecutionScheduler,
      Duration admissionTimeout,
      SemanticMetrics metrics) {
    this.provider = Objects.requireNonNull(provider, "provider must not be null");
    this.semanticCache = Objects.requireNonNull(semanticCache, "semanticCache must not be null");
    this.chatCompletion = Objects.requireNonNull(chatCompletion, "chatCompletion must not be null");
    this.proactiveToolRouter =
        Objects.requireNonNull(proactiveToolRouter, "proactiveToolRouter must not be null");
    this.modelExecutionScheduler =
        Objects.requireNonNull(modelExecutionScheduler, "modelExecutionScheduler must not be null");
    this.admissionTimeout =
        Objects.requireNonNull(admissionTimeout, "admissionTimeout must not be null");
    this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
    if (admissionTimeout.isZero() || admissionTimeout.isNegative()) {
      throw new IllegalArgumentException("admissionTimeout must be positive");
    }
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
    String response;
    try {
      response =
          chatCompletion
              .map(chat -> chat.complete(conversationContext, userMessage))
              .orElseGet(() -> executeLegacyChat(conversationContext, userMessage));
    } catch (ChatProviderUnavailableException unavailable) {
      response = executeLegacyChat(conversationContext, userMessage);
    }
    String completedResponse = response;
    try {
      semanticCache.ifPresent(
          cache -> cache.store(conversationContext, userMessage, completedResponse));
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      recordFallback("semantic_cache_write_failure");
      // Semantic caching is an optimization and must not make chat unavailable.
    }
    return completedResponse;
  }

  private void recordFallback(String reason) {
    try {
      metrics.recordFallback("chat", reason);
    } catch (RuntimeException ignored) {
      // Observability must not change chat semantics.
    }
  }

  private String executeLegacyChat(String conversationContext, String userMessage) {
    if (modelExecutionScheduler.isEmpty()) {
      return provider.chat(conversationContext, userMessage);
    }
    return modelExecutionScheduler
        .orElseThrow()
        .execute(
            ModelCapability.GENERATION,
            AiExecutionContextScope.requireCurrent(),
            admissionTimeout,
            () -> provider.chat(conversationContext, userMessage));
  }
}
