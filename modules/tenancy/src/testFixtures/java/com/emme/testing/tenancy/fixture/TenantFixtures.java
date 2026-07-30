package com.emme.testing.tenancy.fixture;

import java.util.UUID;

/**
 * Pre-built tenant fixtures for integration tests.
 *
 * <p>Each constant has a fixed UUID for deterministic, reusable tenants. Use {@link
 * TenantFixture#unique(String)} when a test needs isolation.
 */
public final class TenantFixtures {

  private TenantFixtures() {
    throw new UnsupportedOperationException("Constants only");
  }

  public static final TenantFixture STUDIO_A =
      new TenantFixture(
          UUID.fromString("11111111-1111-1111-1111-111111111111"),
          "studio-a",
          "Studio A — Premium Salon");

  public static final TenantFixture SALON_B =
      new TenantFixture(
          UUID.fromString("22222222-2222-2222-2222-222222222222"), "salon-b", "Salon B — Basic");

  public static final TenantFixture ENTERPRISE_C =
      new TenantFixture(
          UUID.fromString("33333333-3333-3333-3333-333333333333"),
          "enterprise-c",
          "Enterprise C — Multi-location");
}
