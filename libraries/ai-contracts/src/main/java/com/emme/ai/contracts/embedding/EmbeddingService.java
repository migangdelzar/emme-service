package com.emme.ai.contracts.embedding;

import java.util.List;

/** Cross-module application capability for embedding text. */
@FunctionalInterface
public interface EmbeddingService {

  List<Float> embed(String text);
}
