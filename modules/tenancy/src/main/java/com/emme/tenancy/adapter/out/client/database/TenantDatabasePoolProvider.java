package com.emme.tenancy.adapter.out.client.database;

import com.emme.kernel.context.TenantContextHolder;
import com.emme.tenancy.application.port.out.DatabaseRegistryEntry;
import com.emme.tenancy.application.port.out.DatabaseRegistryPort;
import com.emme.tenancy.configuration.TenantDatabaseConnectionProperties;
import com.emme.tenancy.configuration.TenantPoolingProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Ticker;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Manages HikariCP connection pools per database.
 *
 * <p>Lazily creates pools via {@link #getDataSource()}, uses Caffeine for automatic idle-pool
 * eviction, and applies dynamic sizing at pool creation. The default database pool is held
 * separately and never evicted.
 */
@Component
public class TenantDatabasePoolProvider {

  private static final Logger log = LoggerFactory.getLogger(TenantDatabasePoolProvider.class);

  private final TenantPoolingProperties config;
  private final TenantDatabaseConnectionProperties connectionProperties;
  private final DatabaseRegistryPort databaseRegistryPort;

  /** Caffeine cache for tenant database pools — auto-evicts on idle timeout. */
  private final Cache<UUID, HikariDataSource> poolCache;

  /** Default database pool — held separately, never evicted. */
  private final AtomicReference<HikariDataSource> defaultPoolRef = new AtomicReference<>();

  @Autowired
  public TenantDatabasePoolProvider(
      TenantPoolingProperties config,
      TenantDatabaseConnectionProperties connectionProperties,
      DatabaseRegistryPort databaseRegistryPort) {
    this(config, connectionProperties, databaseRegistryPort, Ticker.systemTicker());
  }

  TenantDatabasePoolProvider(
      TenantPoolingProperties config,
      TenantDatabaseConnectionProperties connectionProperties,
      DatabaseRegistryPort databaseRegistryPort,
      Ticker ticker) {
    this.config = config;
    this.connectionProperties = connectionProperties;
    this.databaseRegistryPort = databaseRegistryPort;

    this.poolCache =
        Caffeine.newBuilder()
            .ticker(ticker)
            .expireAfterAccess(config.getIdleTimeoutMinutes(), TimeUnit.MINUTES)
            .maximumSize(config.getMaxPoolCacheSize())
            .removalListener(
                (UUID key, HikariDataSource ds, RemovalCause cause) -> {
                  if (ds != null && !ds.isClosed()) {
                    ds.close();
                    log.info("Evicted idle pool for database {} (cause={})", key, cause);
                  }
                })
            .build();
  }

  /**
   * Returns the HikariDataSource for the current database context.
   *
   * <p>If the current database context is absent or matches the default database, the non-evictable
   * default pool is used. Otherwise, a Caffeine-managed pool is returned (lazy-created on miss).
   */
  public HikariDataSource getDataSource() {
    UUID databaseId = TenantContextHolder.currentDatabaseOptional().orElse(null);
    UUID defaultId = UUID.fromString(config.getDefaultDatabaseId());

    if (databaseId == null || defaultId.equals(databaseId)) {
      // Default database — use non-evictable pool via CAS
      HikariDataSource pool = defaultPoolRef.get();
      if (pool == null || pool.isClosed()) {
        HikariDataSource newPool = createPool(defaultId);
        if (defaultPoolRef.compareAndSet(pool, newPool)) {
          pool = newPool;
        } else {
          // Another thread won the race — close our unused pool
          newPool.close();
          pool = defaultPoolRef.get();
        }
      }
      return pool;
    }

    // Tenant database — Caffeine-managed with idle eviction
    HikariDataSource ds = poolCache.get(databaseId, this::createPool);
    if (poolCache.estimatedSize() > config.getMaxPoolCacheSize() * 0.8) {
      log.warn(
          "Pool cache approaching max size: {}/{}",
          poolCache.estimatedSize(),
          config.getMaxPoolCacheSize());
    }
    return ds;
  }

  /**
   * Creates a new HikariCP pool for the given database.
   *
   * <p>Resolves the JDBC URL and pool sizing from {@link DatabaseRegistryEntry}. Applies dynamic
   * sizing based on the global connection cap and number of active pools. Higher-priority databases
   * receive a connection bonus.
   */
  private HikariDataSource createPool(UUID databaseId) {
    DatabaseRegistryEntry db =
        databaseRegistryPort
            .findById(databaseId)
            .orElseThrow(
                () -> new IllegalStateException("Database not found in registry: " + databaseId));

    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(db.jdbcUrl());
    hikariConfig.setUsername(connectionProperties.getUsername());
    hikariConfig.setPassword(connectionProperties.getPassword());
    hikariConfig.setDriverClassName(connectionProperties.getDriverClassName());

    int minSize = (db.minPoolSize() != null) ? db.minPoolSize() : config.getDefaultMinPoolSize();
    int maxSize = (db.maxPoolSize() != null) ? db.maxPoolSize() : config.getDefaultMaxPoolSize();

    // Dynamic sizing: divide global budget across active pools
    long estimatedActivePools = poolCache.estimatedSize() + 1;
    int dynamicMax =
        (int)
            Math.max(
                minSize,
                Math.ceil((double) config.getGlobalMaxConnections() / estimatedActivePools));
    int effectiveMax = Math.min(maxSize, dynamicMax);

    // Priority bonus for high-priority databases
    if (db.priority() != null && db.priority() > 0) {
      int priorityBonus = db.priority();
      effectiveMax = Math.min(effectiveMax + priorityBonus, maxSize);
    }

    hikariConfig.setMinimumIdle(minSize);
    hikariConfig.setMaximumPoolSize(effectiveMax);
    hikariConfig.setPoolName("emme-pool-" + db.name());

    HikariDataSource ds = new HikariDataSource(hikariConfig);

    log.info(
        "Created pool '{}' (min={}, max={}, priority={}) for database {}",
        hikariConfig.getPoolName(),
        minSize,
        effectiveMax,
        db.priority() != null ? db.priority() : 0,
        databaseId);

    return ds;
  }

  /** Shuts down all pools: default + cached. */
  @PreDestroy
  public void shutdown() {
    HikariDataSource currentDefault = defaultPoolRef.get();
    if (currentDefault != null && !currentDefault.isClosed()) {
      currentDefault.close();
    }
    poolCache
        .asMap()
        .values()
        .forEach(
            ds -> {
              if (!ds.isClosed()) {
                ds.close();
              }
            });
    poolCache.invalidateAll();
    log.info("All connection pools shut down");
  }

  /** Returns the number of active pools (cached + default, for observability). */
  public int getActivePoolCount() {
    int count = (int) poolCache.estimatedSize();
    HikariDataSource currentDefault = defaultPoolRef.get();
    if (currentDefault != null && !currentDefault.isClosed()) {
      count++;
    }
    return count;
  }
}
