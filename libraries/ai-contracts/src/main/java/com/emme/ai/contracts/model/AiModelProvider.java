package com.emme.ai.contracts.model;

import java.util.List;

/**
 * Provider-neutral boundary for model capabilities used by the Assistant application layer.
 *
 * <p>Implementations belong to infrastructure modules. The contract deliberately contains no
 * Spring, HTTP-client, provider SDK, tenant-resolution, or persistence types.
 */
public interface AiModelProvider {

  /** Stable provider identifier used for telemetry and configuration diagnostics. */
  String name();

  /** Chat completion using already-prepared context and user input. */
  String chat(String conversationContext, String userMessage);

  /**
   * Generates an embedding vector. An unavailable capability is represented by an empty list; a
   * caller must not persist a fabricated zero vector.
   */
  List<Float> embed(String text);

  /** Vision captioning for an image represented by a base64 payload. */
  default String caption(String imageBase64) {
    return "maqueta de imagen " + java.util.UUID.randomUUID().toString().substring(0, 8);
  }

  /** Whether the implementation is deterministic test infrastructure. */
  default boolean isMock() {
    return false;
  }
}
