package com.emme.assistant.ai.application.tool;

import com.emme.ai.contracts.embedding.EmbeddingService;
import com.emme.assistant.ai.application.port.out.ProactiveToolRouter;
import com.emme.assistant.ai.application.semantic.SemanticDecision;
import com.emme.assistant.ai.application.semantic.SemanticQuery;
import com.emme.assistant.ai.application.semantic.SemanticToolSelector;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Uses vector similarity to invoke only high-confidence, safe, authorized tools. */
public final class SemanticProactiveToolRouter implements ProactiveToolRouter {

  private final SemanticToolSelector selector;
  private final AiToolGateway gateway;
  private final String locale;
  private final java.util.Optional<EmbeddingService> legacyEmbeddings;

  public SemanticProactiveToolRouter(
      SemanticToolSelector selector, AiToolGateway gateway, String locale) {
    this(selector, gateway, locale, java.util.Optional.empty());
  }

  /**
   * @deprecated use the prepared-query constructor.
   */
  @Deprecated
  public SemanticProactiveToolRouter(
      EmbeddingService embeddings,
      SemanticToolSelector selector,
      AiToolGateway gateway,
      String locale) {
    this(selector, gateway, locale, java.util.Optional.of(Objects.requireNonNull(embeddings)));
  }

  private SemanticProactiveToolRouter(
      SemanticToolSelector selector,
      AiToolGateway gateway,
      String locale,
      java.util.Optional<EmbeddingService> legacyEmbeddings) {
    this.selector = Objects.requireNonNull(selector, "selector must not be null");
    this.gateway = Objects.requireNonNull(gateway, "gateway must not be null");
    if (locale == null || locale.isBlank()) {
      throw new IllegalArgumentException("locale must not be blank");
    }
    this.locale = locale;
    this.legacyEmbeddings =
        Objects.requireNonNull(legacyEmbeddings, "legacyEmbeddings must not be null");
  }

  @Override
  public Optional<AiToolResult> route(SemanticQuery query) {
    Objects.requireNonNull(query, "query must not be null");
    return route(query, gateway.proactivelyEligibleToolKeys());
  }

  private Optional<AiToolResult> route(SemanticQuery query, Set<String> authorizedToolKeys) {
    if (authorizedToolKeys.isEmpty()) {
      return Optional.empty();
    }
    SemanticDecision decision = selector.select(locale, query.embedding(), authorizedToolKeys);
    return decision
        .selectedKey()
        .map(toolKey -> gateway.execute(new AiToolInvocation(toolKey, Map.of(), false, false)));
  }

  /**
   * @deprecated use {@link #route(SemanticQuery)}.
   */
  @Override
  @Deprecated
  public Optional<AiToolResult> route(String userMessage) {
    if (userMessage == null || userMessage.isBlank()) {
      throw new IllegalArgumentException("userMessage must not be blank");
    }
    Set<String> authorizedToolKeys = gateway.proactivelyEligibleToolKeys();
    if (authorizedToolKeys.isEmpty()) {
      return Optional.empty();
    }
    return route(
        new SemanticQuery(
            userMessage,
            legacyEmbeddings
                .orElseThrow(() -> new IllegalStateException("A semantic query is required"))
                .embed(userMessage)),
        authorizedToolKeys);
  }
}
