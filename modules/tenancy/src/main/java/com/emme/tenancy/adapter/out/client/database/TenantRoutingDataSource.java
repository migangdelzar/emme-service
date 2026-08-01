package com.emme.tenancy.adapter.out.client.database;

import com.emme.kernel.context.TenantContextHolder;
import com.emme.tenancy.configuration.TenantPoolingProperties;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

/**
 * Spring {@link AbstractRoutingDataSource} that resolves the current tenant's database UUID via
 * {@link TenantContextHolder} and delegates connection acquisition to {@link
 * TenantDatabasePoolProvider}.
 *
 * <p>Unlike standard {@code AbstractRoutingDataSource} usage, this implementation does not
 * pre-register all possible DataSources. Instead, it overrides {@link #determineTargetDataSource()}
 * to call {@link TenantDatabasePoolProvider#getDataSource()} directly, enabling lazy pool creation
 * per tenant database.
 */
@Component
@ConditionalOnExpression("!'${spring.datasource.url:}'.contains('h2')")
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

  private final TenantDatabasePoolProvider poolProvider;
  private final TenantPoolingProperties config;

  @SuppressWarnings("this-escape")
  public TenantRoutingDataSource(
      TenantDatabasePoolProvider poolProvider, TenantPoolingProperties config) {
    this.poolProvider = poolProvider;
    this.config = config;

    // AbstractRoutingDataSource requires a non-null targetDataSources map.
    // We override determineTargetDataSource() so the map content is irrelevant.
    setTargetDataSources(Map.of());
    // Deliberately NOT calling afterPropertiesSet() — it eagerly resolves
    // the target DataSource via determineTargetDataSource() which would
    // trigger TenantDatabasePoolProvider → JPA → circular dependency.
    // Resolution happens lazily on first connection request instead.
  }

  @Override
  public void afterPropertiesSet() {
    // Must call super to satisfy DataSourceHealthContributor validation.
    // The default DB pool is resolved lazily via determineTargetDataSource().
    // DatabaseRegistryAdapter hardcodes the default DB entry so no JPA cycle occurs.
    super.afterPropertiesSet();
  }

  /**
   * Returns the database UUID that identifies which connection pool to use.
   *
   * <p>Falls back to {@link TenantPoolingProperties#getDefaultDatabaseId()} when the current thread
   * has no tenant context set.
   */
  @Override
  protected Object determineCurrentLookupKey() {
    return TenantContextHolder.currentDatabaseOptional()
        .orElseGet(() -> UUID.fromString(config.getDefaultDatabaseId()));
  }

  /**
   * Resolves the target DataSource by delegating to {@link TenantDatabasePoolProvider}.
   *
   * <p>This override bypasses the standard {@code resolvedDataSources} map lookup because pools are
   * created lazily at runtime and cannot be pre-registered. {@link TenantDatabasePoolProvider}
   * handles creation, caching, eviction, and dynamic resizing of HikariCP pools.
   */
  @Override
  protected DataSource determineTargetDataSource() {
    return poolProvider.getDataSource();
  }
}
