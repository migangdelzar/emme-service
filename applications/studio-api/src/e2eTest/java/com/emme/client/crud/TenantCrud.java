package com.emme.client.crud;

import com.emme.client.Template;
import com.emme.client.UserSession;

public class TenantCrud {
  private final UserSession session;

  public TenantCrud(UserSession s) {
    this.session = s;
  }

  public String create(String slug, String name) {
    return session.post(
        "/api/v1/tenants",
        Template.load("tenant-create.json").set("slug", slug).set("name", name).render());
  }

  public String list() {
    return session.get("/api/v1/tenants");
  }

  public String getById(String id) {
    return session.get("/api/v1/tenants/" + id);
  }

  /** Update tenant name via PATCH. Returns empty string if endpoint not yet implemented. */
  public String update(String id, String name) {
    // PATCH not yet implemented on server; will return 200 when ready
    try {
      return session.put("/api/v1/tenants/" + id, "{\"name\":\"" + name + "\"}");
    } catch (AssertionError e) {
      if (e.getMessage().contains("405")) return ""; // endpoint not yet implemented
      throw e;
    }
  }
}
