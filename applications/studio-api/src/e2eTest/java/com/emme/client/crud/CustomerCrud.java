package com.emme.client.crud;

import com.emme.client.Template;
import com.emme.client.UserSession;

public class CustomerCrud {
  private final UserSession session;

  public CustomerCrud(UserSession s) {
    this.session = s;
  }

  public String create(String name, String email, String phone) {
    return session.post(
        "/api/v1/customers",
        Template.load("customer-create.json")
            .set("name", name)
            .set("email", email)
            .set("phone", phone)
            .render());
  }

  public String list() {
    return session.get("/api/v1/customers");
  }

  public String getById(String id) {
    return session.get("/api/v1/customers/" + id);
  }

  public String search(String query) {
    return session.get("/api/v1/customers/search?q=" + query);
  }
}
