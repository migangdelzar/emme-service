package com.emme.assistant.ai.application.port.out;

import java.util.List;
import java.util.Map;

/**
 * Abstraction for AI model providers (Ollama, OpenAI, Groq, Mock). Implementations handle chat
 * completion, embedding, and intent routing.
 */
public interface ModelProvider {

  /** Provider identifier */
  String name();

  /** Chat completion — context + user message → AI response */
  String chat(String conversationContext, String userMessage);

  /**
   * Generate an embedding vector for text. Returns exactly {@code app.ai.embedding.dimension}
   * floats, or an EMPTY list when embedding is unavailable (callers must leave the row unembedded,
   * never store zero vectors — cosine distance is undefined for them).
   */
  List<Float> embed(String text);

  /** Detect intent from user message */
  IntentResult routeIntent(String message);

  /** Vision captioning: describe an image (base64-encoded) in natural language. */
  default String caption(String imageBase64) {
    return "maqueta de imagen " + java.util.UUID.randomUUID().toString().substring(0, 8);
  }

  /** Whether this provider is a real (non-mock) implementation */
  default boolean isMock() {
    return false;
  }

  /** Intent result DTO */
  record IntentResult(String intent, double confidence, Map<String, String> parameters) {}
}
