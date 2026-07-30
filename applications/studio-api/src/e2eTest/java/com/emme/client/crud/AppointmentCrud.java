package com.emme.client.crud;

import com.emme.client.Template;
import com.emme.client.UserSession;

public class AppointmentCrud {
  private final UserSession session;

  public AppointmentCrud(UserSession s) {
    this.session = s;
  }

  public String create(
      String customerId, String serviceId, String artistId, String startsAt, String endsAt) {
    return session.post(
        "/api/v1/appointments",
        Template.load("appointment-create.json")
            .set("customerId", customerId)
            .set("serviceId", serviceId)
            .set("artistId", artistId)
            .set("startsAt", startsAt)
            .set("endsAt", endsAt)
            .render());
  }

  public String list() {
    return session.get("/api/v1/appointments");
  }

  public String cancel(String id) {
    return session.post("/api/v1/appointments/" + id + "/cancel", "{}");
  }
}
