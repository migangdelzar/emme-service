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
        "/api/tenants",
        Template.load("tenant-create.json").set("slug", slug).set("name", name).render());
  }

  public String list() {
    return session.get("/api/tenants");
  }

  public String getById(String id) {
    return session.get("/api/tenants/" + id);
  }

  /** Update tenant name via PATCH. */
  public String update(String id, String name) {
    return session.patch("/api/tenants/" + id, "{\"name\":\"" + name + "\"}");
  }
}
