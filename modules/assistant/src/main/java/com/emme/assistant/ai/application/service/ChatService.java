package com.emme.assistant.ai.application.service;

import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.ai.contracts.guardrail.InputRequest;
import com.emme.ai.contracts.guardrail.OutputRequest;
import com.emme.ai.contracts.model.AiChatCompletion;
import com.emme.ai.contracts.model.ChatResponse;
import com.emme.assistant.ai.api.usecase.ChatUseCase;
import com.emme.assistant.ai.application.guardrail.GuardrailRejectedException;
import com.emme.assistant.ai.application.guardrail.InputGuard;
import com.emme.assistant.ai.application.guardrail.OutputGuard;
import com.emme.assistant.ai.application.port.out.NoopSemanticMetrics;
import com.emme.assistant.ai.application.port.out.ProactiveToolRouter;
import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticResponseCache;
import com.emme.assistant.ai.application.semantic.SemanticCacheIdentity;
import com.emme.assistant.ai.application.semantic.SemanticFailurePolicy;
import com.emme.assistant.ai.application.semantic.SemanticQuery;
import com.emme.assistant.ai.application.semantic.SemanticQueryFactory;
import com.emme.assistant.ai.application.tool.AiToolResult;
import com.emme.kernel.context.AiExecutionContextScope;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Executes the chat capability through the configured model-provider boundary. */
@Service
public class ChatService implements ChatUseCase {
  private static final AiChatCompletion.ProviderPolicy TEST_PROVIDER_POLICY =
      new AiChatCompletion.ProviderPolicy(List.of("test"), true);

  private final AiChatCompletion chatCompletion;
  private final AiChatCompletion.ProviderPolicy providerPolicy;
  private final Optional<SemanticResponseCache> semanticCache;
  private final Optional<ProactiveToolRouter> proactiveToolRouter;
  private final Optional<SemanticQueryFactory> semanticQueryFactory;
  private final SemanticMetrics metrics;
  private final Optional<InputGuard> inputGuard;
  private final Optional<OutputGuard> outputGuard;

  public ChatService(
      AiChatCompletion chatCompletion, Optional<SemanticResponseCache> semanticCache) {
    this(chatCompletion, semanticCache, Optional.empty(), NoopSemanticMetrics.INSTANCE);
  }

  public ChatService(
      AiChatCompletion chatCompletion,
      Optional<SemanticResponseCache> semanticCache,
      Optional<ProactiveToolRouter> proactiveToolRouter) {
    this(chatCompletion, semanticCache, proactiveToolRouter, NoopSemanticMetrics.INSTANCE);
  }

  public ChatService(
      AiChatCompletion chatCompletion,
      Optional<SemanticResponseCache> semanticCache,
      Optional<ProactiveToolRouter> proactiveToolRouter,
      SemanticMetrics metrics) {
    this(chatCompletion, semanticCache, proactiveToolRouter, Optional.empty(), metrics);
  }

  public ChatService(
      AiChatCompletion chatCompletion,
      Optional<SemanticResponseCache> semanticCache,
      Optional<ProactiveToolRouter> proactiveToolRouter,
      Optional<SemanticQueryFactory> semanticQueryFactory,
      SemanticMetrics metrics) {
    this(
        chatCompletion,
        semanticCache,
        proactiveToolRouter,
        semanticQueryFactory,
        metrics,
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
  }

  @Autowired
  public ChatService(
      AiChatCompletion chatCompletion,
      Optional<SemanticResponseCache> semanticCache,
      Optional<ProactiveToolRouter> proactiveToolRouter,
      Optional<SemanticQueryFactory> semanticQueryFactory,
      SemanticMetrics metrics,
      Optional<InputGuard> inputGuard,
      Optional<OutputGuard> outputGuard,
      Optional<AiChatCompletion.ProviderPolicy> providerPolicy) {
    this.chatCompletion = Objects.requireNonNull(chatCompletion, "chatCompletion must not be null");
    this.providerPolicy =
        Objects.requireNonNull(providerPolicy, "providerPolicy must not be null")
            .orElse(TEST_PROVIDER_POLICY);
    this.semanticCache = Objects.requireNonNull(semanticCache, "semanticCache must not be null");
    this.proactiveToolRouter =
        Objects.requireNonNull(proactiveToolRouter, "proactiveToolRouter must not be null");
    this.semanticQueryFactory =
        Objects.requireNonNull(semanticQueryFactory, "semanticQueryFactory must not be null");
    this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
    this.inputGuard = Objects.requireNonNull(inputGuard, "inputGuard must not be null");
    this.outputGuard = Objects.requireNonNull(outputGuard, "outputGuard must not be null");
  }

  @Override
  public String chat(String conversationContext, String userMessage) {
    var context = AiExecutionContextScope.requireCurrent();
    checkInput(context, userMessage);
    Optional<SemanticQuery> semanticQuery = prepareQuery(userMessage, context);
    Optional<AiToolResult> proactiveToolResult;
    try {
      proactiveToolResult =
          proactiveToolRouter.flatMap(router -> semanticQuery.flatMap(router::route));
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      recordFallback("semantic_tool_failure");
      proactiveToolResult = Optional.empty();
    }
    if (proactiveToolResult.isPresent()) {
      return checkOutput(context, proactiveToolResult.orElseThrow().content());
    }
    Optional<String> cached;
    try {
      cached =
          semanticCache.flatMap(
              cache ->
                  semanticQuery
                      .map(query -> cache.lookup(conversationContext, query))
                      .orElseGet(Optional::empty));
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      recordFallback("semantic_cache_failure");
      cached = Optional.empty();
    }
    if (cached.isPresent()) {
      return checkOutput(context, cached.orElseThrow());
    }
    Completion response = complete(chatCompletion, conversationContext, userMessage, context);
    Completion completed = response;
    String completedResponse = completed.content();
    try {
      semanticCache.ifPresent(
          cache -> {
            if (semanticQuery.isPresent()) {
              SemanticQuery query = semanticQuery.orElseThrow();
              cache.store(
                  conversationContext,
                  query,
                  completedResponse,
                  new SemanticCacheIdentity(
                      completed.provider(),
                      completed.model(),
                      "knowledge-v1",
                      "policy-v1",
                      "source-v1"));
            }
          });
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      recordFallback("semantic_cache_write_failure");
      // Semantic caching is an optimization and must not make chat unavailable.
    }
    return checkOutput(context, completedResponse);
  }

  private void checkInput(com.emme.kernel.context.AiExecutionContext context, String userMessage) {
    if (inputGuard.isEmpty() || userMessage == null || userMessage.isBlank()) {
      return;
    }
    GuardrailDecision decision =
        inputGuard
            .orElseThrow()
            .check(
                new InputRequest(
                    userMessage,
                    userMessage.getBytes(StandardCharsets.UTF_8).length,
                    0,
                    context.idempotencyKey()),
                context);
    if (decision.action() != GuardrailAction.ALLOW) {
      throw new GuardrailRejectedException(decision);
    }
  }

  private String checkOutput(com.emme.kernel.context.AiExecutionContext context, String response) {
    if (outputGuard.isEmpty()) {
      return response;
    }
    GuardrailDecision decision =
        outputGuard
            .orElseThrow()
            .check(
                new OutputRequest(
                    context.channel().name().toLowerCase(Locale.ROOT), response, false, false),
                context);
    if (decision.action() != GuardrailAction.ALLOW) {
      throw new GuardrailRejectedException(decision);
    }
    return response;
  }

  private Optional<SemanticQuery> prepareQuery(
      String userMessage, com.emme.kernel.context.AiExecutionContext context) {
    if (semanticCache.isEmpty() && proactiveToolRouter.isEmpty()) {
      return Optional.empty();
    }
    if (semanticQueryFactory.isEmpty()) {
      recordFallback("semantic_query_unavailable");
      return Optional.empty();
    }
    try {
      return Optional.of(semanticQueryFactory.orElseThrow().create(userMessage, context));
    } catch (RuntimeException failure) {
      SemanticFailurePolicy.rethrowSecurityFailure(failure);
      recordFallback("semantic_query_failure");
      return Optional.empty();
    }
  }

  private Completion complete(
      AiChatCompletion chat,
      String conversationContext,
      String userMessage,
      com.emme.kernel.context.AiExecutionContext context) {
    ChatResponse response =
        chat.complete(
            new AiChatCompletion.Request(
                conversationContext, userMessage, context, providerPolicy));
    return new Completion(response.content(), response.provider(), response.modelVersion());
  }

  private record Completion(String content, String provider, String model) {}

  private void recordFallback(String reason) {
    try {
      metrics.recordFallback("chat", reason);
    } catch (RuntimeException ignored) {
      // Observability must not change chat semantics.
    }
  }
}
