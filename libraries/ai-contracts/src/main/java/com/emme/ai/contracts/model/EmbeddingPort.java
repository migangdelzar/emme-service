package com.emme.ai.contracts.model;

import com.emme.ai.contracts.semantic.EmbeddingModelVersion;
import com.emme.ai.contracts.semantic.EmbeddingVector;

/** Provider-neutral embedding port with explicit embedding-space metadata. */
public interface EmbeddingPort {

  EmbeddingVector embed(String input, EmbeddingModelVersion modelVersion);
}
