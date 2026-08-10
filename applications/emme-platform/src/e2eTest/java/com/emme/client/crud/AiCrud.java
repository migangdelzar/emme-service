package com.emme.client.crud;

import com.emme.client.UserSession;

public class AiCrud {
  private final UserSession session;

  public AiCrud(UserSession s) {
    this.session = s;
  }

  public String chat(String message) {
    return session.post("/api/ai/chat", "{\"userMessage\":\"" + message + "\"}", 200);
  }
}
