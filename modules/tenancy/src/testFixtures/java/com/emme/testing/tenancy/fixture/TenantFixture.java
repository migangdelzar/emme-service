package com.emme.testing.tenancy.fixture;

import java.util.UUID;

/**
 * Immutable tenant test data.
 *
 * <p>Use {@link TenantFixtures} for pre-built constants or create unique instances via the
 * canonical constructor.
 */
public record TenantFixture(UUID id, String slug, String displayName) {

  /** Creates a fixture with a random UUID. */
  public static TenantFixture unique(String slugPrefix) {
    var id = UUID.randomUUID();
    return new TenantFixture(id, slugPrefix + "-" + id, "Integration Test: " + slugPrefix);
  }

  public UUID toId() {
    return id;
  }
}
