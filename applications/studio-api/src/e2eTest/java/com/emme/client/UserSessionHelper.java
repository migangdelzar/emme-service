package com.emme.client;

/**
 * Fluent helper built on top of UserSession and Template. Encapsulates common E2E test patterns:
 * create tenant → create subscription → create resources.
 */
public class UserSessionHelper {

  private final UserSession session;
  private String defaultTenantId;
  private String defaultArtistId;

  public UserSessionHelper(UserSession session) {
    this.session = session;
  }

  /** Create a tenant and return its slug. */
  public String createTenant(String slug, String name) {
    var body = Template.load("tenant-create.json").set("slug", slug).set("name", name).render();
    session.post("/api/v1/tenants", body);
    return slug;
  }

  /** Ensure tenant has an ENTERPRISE subscription (idempotent). */
  public void ensureSubscription(String tenantId) {
    try {
      var body =
          Template.load("subscription-create.json")
              .set("tenantId", tenantId)
              .set("plan", "ENTERPRISE")
              .render();
      session.post("/api/v1/subscriptions", body);
    } catch (AssertionError e) {
      // Already exists — ignore
    }
    this.defaultTenantId = tenantId;
  }

  /** Create a customer. */
  public String createCustomer(String name, String email, String phone) {
    var body =
        Template.load("customer-create.json")
            .set("name", name)
            .set("email", email)
            .set("phone", phone)
            .render();
    return session.post("/api/v1/customers", body);
  }

  /** Create a service. */
  public String createService(String name, String code, int price, int duration, String category) {
    var body =
        Template.load("service-create.json")
            .set("name", name)
            .set("code", code)
            .set("price", price)
            .set("duration", duration)
            .set("category", category)
            .render();
    return session.post("/api/v1/services", body);
  }

  /** Create an artist. */
  public String createArtist(String name) {
    var body = Template.load("artist-create.json").set("name", name).render();
    return session.post("/api/v1/artists", body);
  }

  /** List tenants. */
  public String listTenants() {
    return session.get("/api/v1/tenants");
  }

  /** List customers. */
  public String listCustomers() {
    return session.get("/api/v1/customers");
  }

  /** Get current session. */
  public UserSession session() {
    return session;
  }
}
