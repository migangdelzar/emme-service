package com.emme.tenancy.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound capability for resolving database connection metadata.
 *
 * <p>Pool management depends on this abstraction and never on JPA entities, Spring Data
 * repositories, or bootstrap JDBC details.
 */
public interface DatabaseRegistryPort {

  Optional<DatabaseRegistryEntry> findById(UUID databaseId);
}
