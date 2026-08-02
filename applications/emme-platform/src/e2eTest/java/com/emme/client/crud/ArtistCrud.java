package com.emme.client.crud;

import com.emme.client.Template;
import com.emme.client.UserSession;

public class ArtistCrud {
  private final UserSession session;

  public ArtistCrud(UserSession s) {
    this.session = s;
  }

  public String create(String name) {
    return session.post(
        "/api/artists", Template.load("artist-create.json").set("name", name).render());
  }

  public String list() {
    return session.get("/api/artists");
  }

  public String getById(String id) {
    return session.get("/api/artists/" + id);
  }
}
