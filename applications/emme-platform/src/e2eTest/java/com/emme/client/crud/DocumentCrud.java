package com.emme.client.crud;

import com.emme.client.UserSession;

public class DocumentCrud {
  private final UserSession session;

  public DocumentCrud(UserSession s) {
    this.session = s;
  }

  public String list() {
    return session.get("/api/v1/documents");
  }

  public String create(String name) {
    return session.post(
        "/api/v1/documents", "{\"name\":\"" + name + "\",\"sourceType\":\"MANUAL\"}");
  }
}
