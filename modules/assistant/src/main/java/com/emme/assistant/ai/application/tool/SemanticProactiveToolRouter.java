package com.emme.assistant.ai.application.tool;

import com.emme.assistant.ai.application.port.out.EmbeddingModelPort;
import com.emme.assistant.ai.application.port.out.ProactiveToolRouter;
import com.emme.assistant.ai.application.semantic.EmbeddingVector;
import com.emme.assistant.ai.application.semantic.SemanticDecision;
import com.emme.assistant.ai.application.semantic.SemanticToolSelector;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Uses vector similarity to invoke only high-confidence, safe, authorized tools. */
public final class SemanticProactiveToolRouter implements ProactiveToolRouter {

  private final EmbeddingModelPort embeddings;
  private final SemanticToolSelector selector;
  private final AiToolGateway gateway;
  private final String locale;

  public SemanticProactiveToolRouter(
      EmbeddingModelPort embeddings,
      SemanticToolSelector selector,
      AiToolGateway gateway,
      String locale) {
    this.embeddings = Objects.requireNonNull(embeddings, "embeddings must not be null");
    this.selector = Objects.requireNonNull(selector, "selector must not be null");
    this.gateway = Objects.requireNonNull(gateway, "gateway must not be null");
    if (locale == null || locale.isBlank()) {
      throw new IllegalArgumentException("locale must not be blank");
    }
    this.locale = locale;
  }

  @Override
  public Optional<AiToolResult> route(String userMessage) {
    if (userMessage == null || userMessage.isBlank()) {
      throw new IllegalArgumentException("userMessage must not be blank");
    }
    Set<String> authorizedToolKeys = gateway.proactivelyEligibleToolKeys();
    if (authorizedToolKeys.isEmpty()) {
      return Optional.empty();
    }
    EmbeddingVector query = embeddings.embed(userMessage);
    SemanticDecision decision = selector.select(locale, query, authorizedToolKeys);
    return decision
        .selectedKey()
        .map(toolKey -> gateway.execute(new AiToolInvocation(toolKey, Map.of(), false, false)));
  }
}
