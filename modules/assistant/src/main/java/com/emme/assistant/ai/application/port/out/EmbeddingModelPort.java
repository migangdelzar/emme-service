package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.application.semantic.EmbeddingVector;

/**
 * Compatibility port retained while assistant semantic values migrate to the shared embedding
 * contract.
 *
 * @deprecated migrate callers to {@code com.emme.ai.contracts.embedding.EmbeddingService}
 */
@Deprecated
public interface EmbeddingModelPort {

  /**
   * Embeds the supplied text using the configured model version.
   *
   * <p>Implementations must reject vectors whose dimension does not match the configured
   * persistence dimension before the vector reaches a search or persistence adapter.
   */
  EmbeddingVector embed(String text);
}
