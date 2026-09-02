package com.emme.ai.platform.configuration;

import io.micrometer.common.KeyValues;
import java.util.Objects;
import org.springframework.ai.embedding.observation.DefaultEmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;

/** Small Spring AI observation customization for model names absent from embedding requests. */
public final class SpringAiObservationConventions {

  private SpringAiObservationConventions() {}

  public static EmbeddingModelObservationConvention embeddingModel(String modelName) {
    return new ConfiguredEmbeddingModelObservationConvention(modelName);
  }

  private static final class ConfiguredEmbeddingModelObservationConvention
      implements EmbeddingModelObservationConvention {
    private final String modelName;
    private final DefaultEmbeddingModelObservationConvention delegate =
        new DefaultEmbeddingModelObservationConvention();

    private ConfiguredEmbeddingModelObservationConvention(String modelName) {
      this.modelName = requireText(modelName, "modelName");
    }

    @Override
    public String getName() {
      return delegate.getName();
    }

    @Override
    public String getContextualName(EmbeddingModelObservationContext context) {
      return delegate.getContextualName(context);
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(EmbeddingModelObservationContext context) {
      return delegate.getLowCardinalityKeyValues(context).and("gen_ai.request.model", modelName);
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(EmbeddingModelObservationContext context) {
      return delegate.getHighCardinalityKeyValues(context);
    }

    private static String requireText(String value, String field) {
      Objects.requireNonNull(value, field + " must not be null");
      if (value.isBlank()) {
        throw new IllegalArgumentException(field + " must not be blank");
      }
      return value;
    }
  }
}
