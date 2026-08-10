package com.emme.client.crud;

import com.emme.client.UserSession;

public class BusinessConfigCrud {
  private final UserSession session;

  public BusinessConfigCrud(UserSession s) {
    this.session = s;
  }

  public String profile() {
    return session.get("/api/business-config/profile");
  }

  public String hours() {
    return session.get("/api/business-config/hours");
  }
}
