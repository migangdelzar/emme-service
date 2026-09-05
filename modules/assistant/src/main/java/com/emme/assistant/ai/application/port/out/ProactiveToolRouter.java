package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.application.semantic.SemanticQuery;
import com.emme.assistant.ai.application.tool.AiToolResult;
import java.util.Optional;

/** Attempts a safe deterministic tool route before model-backed chat. */
public interface ProactiveToolRouter {

  Optional<AiToolResult> route(SemanticQuery query);

  /**
   * @deprecated use {@link #route(SemanticQuery)} so callers can reuse a prepared embedding.
   */
  @Deprecated
  default Optional<AiToolResult> route(String userMessage) {
    throw new UnsupportedOperationException("A prepared semantic query is required");
  }
}
