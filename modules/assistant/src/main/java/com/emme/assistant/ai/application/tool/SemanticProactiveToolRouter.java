package com.emme.assistant.ai.application.tool;

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

  public SemanticProactiveToolRouter(
      SemanticToolSelector selector, AiToolGateway gateway, String locale) {
    this.selector = Objects.requireNonNull(selector, "selector must not be null");
    this.gateway = Objects.requireNonNull(gateway, "gateway must not be null");
    if (locale == null || locale.isBlank()) {
      throw new IllegalArgumentException("locale must not be blank");
    }
    this.locale = locale;
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
}
