package com.emme.ai.contracts.embedding;

import com.emme.ai.contracts.semantic.EmbeddingVector;

/** Cross-module application capability for embedding text. */
@FunctionalInterface
public interface EmbeddingService {

  EmbeddingVector embed(String text);
}
