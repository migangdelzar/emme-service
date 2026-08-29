package com.emme.ai.platform.model;

import com.emme.ai.contracts.model.ModelCapability;

/** Conservative capacity limits for one local model host. */
public record ModelCapacityProfile(
    int globalLimit,
    int generationLimit,
    int embeddingLimit,
    int tenantLimit,
    int userLimit,
    int queueCapacity) {

  public ModelCapacityProfile {
    if (globalLimit <= 0
        || generationLimit <= 0
        || embeddingLimit <= 0
        || tenantLimit <= 0
        || userLimit <= 0
        || queueCapacity < 0) {
      throw new IllegalArgumentException("capacity limits must be positive and queue may be zero");
    }
  }

  int capabilityLimit(ModelCapability capability) {
    return switch (capability) {
      case GENERATION, VISION -> generationLimit;
      case EMBEDDING -> embeddingLimit;
    };
  }
}
