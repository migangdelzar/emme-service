package com.emme.client.crud;

import com.emme.client.Template;
import com.emme.client.UserSession;

public class SubscriptionCrud {
  private final UserSession session;

  public SubscriptionCrud(UserSession s) {
    this.session = s;
  }

  public String create(String tenantId, String plan) {
    return session.post(
        "/api/v1/subscriptions",
        Template.load("subscription-create.json")
            .set("tenantId", tenantId)
            .set("plan", plan)
            .render());
  }

  public String get(String tenantId) {
    return session.get("/api/v1/subscriptions/" + tenantId);
  }

  public String changePlan(String tenantId, String plan) {
    return session.put(
        "/api/v1/subscriptions/" + tenantId + "/plan", "{\"plan\":\"" + plan + "\"}");
  }
}
