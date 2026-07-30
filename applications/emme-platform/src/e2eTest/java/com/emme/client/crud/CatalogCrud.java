package com.emme.client.crud;

import com.emme.client.UserSession;

public class CatalogCrud {
  private final UserSession session;

  public CatalogCrud(UserSession s) {
    this.session = s;
  }

  public String list() {
    return session.get("/api/v1/catalog/items");
  }
}
