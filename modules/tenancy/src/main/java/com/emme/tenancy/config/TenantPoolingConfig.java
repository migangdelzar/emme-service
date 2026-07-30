package com.emme.tenancy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "emme.tenancy.pooling")
public class TenantPoolingConfig {

  /** Maximum total connections across ALL database pools */
  private int globalMaxConnections = 200;

  /** Minutes of inactivity before an idle pool is evicted */
  private int idleTimeoutMinutes = 30;

  /**
   * How often the eviction checker runs (seconds). Reserved for future use — Caffeine self-manages
   * eviction internally.
   */
  private int evictionCheckIntervalSeconds = 60;

  /** Minimum pool size per database (overridden by database_registry.min_pool_size if set) */
  private int defaultMinPoolSize = 5;

  /** Maximum pool size per database (overridden by database_registry.max_pool_size if set) */
  private int defaultMaxPoolSize = 20;

  /** Maximum number of database pools cached before eviction. Default 100. */
  private int maxPoolCacheSize = 100;

  /** Default database ID — used when tenant has no database_id assigned */
  private String defaultDatabaseId = "00000000-0000-0000-0000-000000000000";

  public int getGlobalMaxConnections() {
    return globalMaxConnections;
  }

  public void setGlobalMaxConnections(int globalMaxConnections) {
    this.globalMaxConnections = globalMaxConnections;
  }

  public int getIdleTimeoutMinutes() {
    return idleTimeoutMinutes;
  }

  public void setIdleTimeoutMinutes(int idleTimeoutMinutes) {
    this.idleTimeoutMinutes = idleTimeoutMinutes;
  }

  public int getEvictionCheckIntervalSeconds() {
    return evictionCheckIntervalSeconds;
  }

  public void setEvictionCheckIntervalSeconds(int evictionCheckIntervalSeconds) {
    this.evictionCheckIntervalSeconds = evictionCheckIntervalSeconds;
  }

  public int getDefaultMinPoolSize() {
    return defaultMinPoolSize;
  }

  public void setDefaultMinPoolSize(int defaultMinPoolSize) {
    this.defaultMinPoolSize = defaultMinPoolSize;
  }

  public int getDefaultMaxPoolSize() {
    return defaultMaxPoolSize;
  }

  public void setDefaultMaxPoolSize(int defaultMaxPoolSize) {
    this.defaultMaxPoolSize = defaultMaxPoolSize;
  }

  public int getMaxPoolCacheSize() {
    return maxPoolCacheSize;
  }

  public void setMaxPoolCacheSize(int maxPoolCacheSize) {
    this.maxPoolCacheSize = maxPoolCacheSize;
  }

  public String getDefaultDatabaseId() {
    return defaultDatabaseId;
  }

  public void setDefaultDatabaseId(String defaultDatabaseId) {
    this.defaultDatabaseId = defaultDatabaseId;
  }
}
