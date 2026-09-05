package com.emme.assistant.ai;

import com.emme.ai.contracts.semantic.DistanceMetric;
import com.emme.ai.contracts.semantic.EmbeddingModelVersion;
import com.emme.ai.contracts.semantic.EmbeddingVector;
import java.util.List;

/** Test factory for canonical vectors used by integration tests. */
final class EmbeddingIntegrationTestVectors {

  private EmbeddingIntegrationTestVectors() {}

  static EmbeddingVector testEmbedding(String modelVersion, List<Float> values) {
    return new EmbeddingVector(
        values,
        new EmbeddingModelVersion(
            modelVersion, modelVersion, values.size(), DistanceMetric.COSINE, "query-v1"));
  }
}
