package com.emme.tenancy.configuration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Typed resource limits and lifecycle settings for tenant database pools. */
@Validated
@ConfigurationProperties(prefix = "emme.tenancy.pooling")
public record TenantPoolingProperties(
    @Min(1) int globalMaxConnections,
    @Min(1) int idleTimeoutMinutes,
    @Min(1) int evictionCheckIntervalSeconds,
    @Min(0) int defaultMinPoolSize,
    @Min(1) int defaultMaxPoolSize,
    @Min(1) int maxPoolCacheSize,
    @NotBlank String defaultDatabaseId) {

  public TenantPoolingProperties(
      @DefaultValue("200") int globalMaxConnections,
      @DefaultValue("30") int idleTimeoutMinutes,
      @DefaultValue("60") int evictionCheckIntervalSeconds,
      @DefaultValue("5") int defaultMinPoolSize,
      @DefaultValue("20") int defaultMaxPoolSize,
      @DefaultValue("100") int maxPoolCacheSize,
      @DefaultValue("00000000-0000-0000-0000-000000000000") String defaultDatabaseId) {
    this.globalMaxConnections = globalMaxConnections;
    this.idleTimeoutMinutes = idleTimeoutMinutes;
    this.evictionCheckIntervalSeconds = evictionCheckIntervalSeconds;
    this.defaultMinPoolSize = defaultMinPoolSize;
    this.defaultMaxPoolSize = defaultMaxPoolSize;
    this.maxPoolCacheSize = maxPoolCacheSize;
    this.defaultDatabaseId = Objects.requireNonNull(defaultDatabaseId, "defaultDatabaseId");
  }

  public static TenantPoolingProperties defaults() {
    return new TenantPoolingProperties(
        200, 30, 60, 5, 20, 100, "00000000-0000-0000-0000-000000000000");
  }

  @AssertTrue(message = "Default minimum pool size must not exceed the default maximum pool size")
  public boolean hasValidDefaultPoolSize() {
    return defaultMinPoolSize <= defaultMaxPoolSize;
  }
}
