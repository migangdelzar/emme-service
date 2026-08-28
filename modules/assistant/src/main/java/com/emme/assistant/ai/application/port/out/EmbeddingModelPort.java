package com.emme.assistant.ai.application.port.out;

import com.emme.assistant.ai.application.service.EmbeddingVector;

/** Provider-neutral port for creating embeddings used by semantic application services. */
public interface EmbeddingModelPort {

  /**
   * Embeds the supplied text using the configured model version.
   *
   * <p>Implementations must reject vectors whose dimension does not match the configured
   * persistence dimension before the vector reaches a search or persistence adapter.
   */
  EmbeddingVector embed(String text);
}
