package com.emme.client.crud;

import com.emme.client.UserSession;

public class PaymentCrud {
  private final UserSession session;

  public PaymentCrud(UserSession s) {
    this.session = s;
  }

  public String create(String providerRef, String amount, String currency) {
    return session.post(
        "/api/payments",
        "{\"providerReference\":\""
            + providerRef
            + "\",\"amount\":"
            + amount
            + ",\"currency\":\""
            + currency
            + "\"}");
  }

  public String list() {
    return session.get("/api/payments");
  }

  public String getById(String id) {
    return session.get("/api/payments/" + id);
  }
}
