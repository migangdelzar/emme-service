package com.emme.ai.platform.configuration;

import com.emme.ai.platform.model.ModelCapacityProfile;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Bounded model-admission settings for the existing local AI provider runtime. */
@ConfigurationProperties("app.ai.model-admission")
public record ModelAdmissionProperties(
    int globalLimit,
    int generationLimit,
    int embeddingLimit,
    int tenantLimit,
    int userLimit,
    int queueCapacity) {

  public ModelAdmissionProperties(
      @DefaultValue("2") int globalLimit,
      @DefaultValue("1") int generationLimit,
      @DefaultValue("2") int embeddingLimit,
      @DefaultValue("1") int tenantLimit,
      @DefaultValue("1") int userLimit,
      @DefaultValue("32") int queueCapacity) {
    requirePositive(globalLimit, "globalLimit");
    requirePositive(generationLimit, "generationLimit");
    requirePositive(embeddingLimit, "embeddingLimit");
    requirePositive(tenantLimit, "tenantLimit");
    requirePositive(userLimit, "userLimit");
    if (queueCapacity < 0) {
      throw new IllegalArgumentException("queueCapacity must not be negative");
    }
    this.globalLimit = globalLimit;
    this.generationLimit = generationLimit;
    this.embeddingLimit = embeddingLimit;
    this.tenantLimit = tenantLimit;
    this.userLimit = userLimit;
    this.queueCapacity = queueCapacity;
  }

  public ModelCapacityProfile profile() {
    return new ModelCapacityProfile(
        globalLimit, generationLimit, embeddingLimit, tenantLimit, userLimit, queueCapacity);
  }

  private static void requirePositive(int value, String field) {
    if (value <= 0) {
      throw new IllegalArgumentException(field + " must be greater than zero");
    }
  }
}
