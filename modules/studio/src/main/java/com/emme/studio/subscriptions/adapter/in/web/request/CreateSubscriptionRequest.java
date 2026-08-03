package com.emme.studio.subscriptions.adapter.in.web.request;

import com.emme.studio.subscriptions.api.command.CreateSubscriptionCommand;
import com.emme.studio.subscriptions.api.type.PlanType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateSubscriptionRequest(@NotNull UUID tenantId, @NotBlank String plan) {
  public CreateSubscriptionCommand toCommand() {
    return new CreateSubscriptionCommand(tenantId, PlanType.valueOf(plan.toUpperCase()));
  }
}
