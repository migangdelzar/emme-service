package com.emme.tenancy.application.port.out;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable database connection metadata required by the tenancy pool adapter.
 *
 * <p>This is an application-facing port model, not a JPA entity or a provider response. It contains
 * only the data required to create and size a connection pool.
 */
public record DatabaseRegistryEntry(
    UUID databaseId,
    String name,
    String jdbcUrl,
    Integer minPoolSize,
    Integer maxPoolSize,
    Integer priority,
    boolean active) {

  public DatabaseRegistryEntry {
    Objects.requireNonNull(databaseId, "databaseId must not be null");
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(jdbcUrl, "jdbcUrl must not be null");
  }
}
