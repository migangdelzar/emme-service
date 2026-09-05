package com.emme.assistant.ai;

import com.emme.ai.contracts.semantic.DistanceMetric;
import com.emme.ai.contracts.semantic.EmbeddingModelVersion;
import com.emme.ai.contracts.semantic.EmbeddingVector;
import java.util.ArrayList;
import java.util.List;

/** Small test factory for vectors that carry the canonical embedding-space identity. */
public final class EmbeddingTestVectors {

  private EmbeddingTestVectors() {}

  public static EmbeddingVector testEmbedding(String modelVersion, float... values) {
    List<Float> components = new ArrayList<>(values.length);
    for (float value : values) {
      components.add(value);
    }
    return testEmbedding(modelVersion, modelVersion, components);
  }

  public static EmbeddingVector testEmbedding(String modelVersion, List<Float> values) {
    return testEmbedding(modelVersion, modelVersion, values);
  }

  public static EmbeddingVector testEmbedding(
      String modelName, String modelVersion, List<Float> values) {
    return new EmbeddingVector(
        values,
        new EmbeddingModelVersion(
            modelName, modelVersion, values.size(), DistanceMetric.COSINE, "query-v1"));
  }
}
