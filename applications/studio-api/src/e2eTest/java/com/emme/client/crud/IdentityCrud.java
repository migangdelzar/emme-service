package com.emme.client.crud;

import com.emme.client.UserSession;

public class IdentityCrud {
  private final UserSession session;

  public IdentityCrud(UserSession s) {
    this.session = s;
  }

  public String me() {
    return session.get("/api/me");
  }

  public String identityMe() {
    return session.get("/api/v1/identity/me");
  }
}
