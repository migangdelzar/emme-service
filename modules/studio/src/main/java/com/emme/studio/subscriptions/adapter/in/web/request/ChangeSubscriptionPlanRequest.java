package com.emme.studio.subscriptions.adapter.in.web.request;

import com.emme.studio.subscriptions.api.command.ChangeSubscriptionPlanCommand;
import com.emme.studio.subscriptions.api.type.PlanType;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChangeSubscriptionPlanRequest(@NotNull String plan) {
  public ChangeSubscriptionPlanCommand toCommand(UUID tenantId) {
    return new ChangeSubscriptionPlanCommand(tenantId, PlanType.valueOf(plan.toUpperCase()));
  }
}
