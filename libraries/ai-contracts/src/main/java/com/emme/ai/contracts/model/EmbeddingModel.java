package com.emme.ai.contracts.model;

import java.util.List;

/**
 * Provider-mechanics compatibility name retained for existing Spring AI adapter wiring.
 *
 * @deprecated use {@code com.emme.ai.contracts.embedding.EmbeddingService} for the raw cross-module
 *     embedding capability
 */
@Deprecated
public interface EmbeddingModel {

  /** Creates an embedding for the supplied text. */
  List<Float> embed(String text);
}
