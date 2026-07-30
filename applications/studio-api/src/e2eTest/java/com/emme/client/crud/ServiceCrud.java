package com.emme.client.crud;

import com.emme.client.Template;
import com.emme.client.UserSession;

public class ServiceCrud {
  private final UserSession session;

  public ServiceCrud(UserSession s) {
    this.session = s;
  }

  public String create(String name, String code, int price, int durationMinutes, String category) {
    return session.post(
        "/api/v1/services",
        Template.load("service-create.json")
            .set("name", name)
            .set("code", code)
            .set("price", price)
            .set("duration", durationMinutes)
            .set("category", category)
            .render());
  }

  public String list() {
    return session.get("/api/v1/services");
  }

  public String getById(String id) {
    return session.get("/api/v1/services/" + id);
  }

  public String listByCategory(String category) {
    return session.get("/api/v1/services?category=" + category);
  }
}
