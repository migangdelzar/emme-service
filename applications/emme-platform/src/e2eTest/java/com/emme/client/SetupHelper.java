package com.emme.client;

/**
 * Fluent setup helper for common test scenarios. Handles subscription creation, full tenant setup,
 * etc.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var api = TestUserContext.current();
 * api.setup().subscription(demoTenantId);       // ensure demo tenant has subscription
 * api.setup().fullStudio("acme-corp");          // create tenant + subscription, returns slug
 * }</pre>
 */
public class SetupHelper {
  private final UserSession session;

  public SetupHelper(UserSession session) {
    this.session = session;
  }

  /** Idempotent: ensure a tenant has an ENTERPRISE subscription. */
  public void subscription(String tenantId) {
    try {
      session.subscriptions().create(tenantId, "ENTERPRISE");
    } catch (AssertionError e) {
      // Already exists — ignore
    }
  }

  /** Create a tenant AND ensure it has a subscription. Returns the tenant's UUID. */
  public String fullStudio(String slug) {
    session.tenants().create(slug, slug + " Studio");
    var tenants = session.tenants().list();
    var id = E2eJson.extract(tenants.contains(slug) ? tenants : session.tenants().list(), "id");
    subscription(id);
    return id;
  }
}
