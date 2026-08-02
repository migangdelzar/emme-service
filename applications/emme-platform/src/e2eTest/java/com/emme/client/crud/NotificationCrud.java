package com.emme.client.crud;

import com.emme.client.UserSession;

public class NotificationCrud {
  private final UserSession session;

  public NotificationCrud(UserSession s) {
    this.session = s;
  }

  public String list() {
    return session.get("/api/notifications");
  }

  public String send(String channel, String recipient, String message) {
    return session.post(
        "/api/notifications",
        "{\"channel\":\""
            + channel
            + "\",\"recipient\":\""
            + recipient
            + "\",\"message\":\""
            + message
            + "\"}");
  }
}
