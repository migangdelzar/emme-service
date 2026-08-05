package com.emme.subscriptions.adapter.in.web.request;

import com.emme.subscriptions.api.command.CreateSubscriptionCommand;
import com.emme.subscriptions.api.type.PlanType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateSubscriptionRequest(@NotNull UUID tenantId, @NotBlank String plan) {
  public CreateSubscriptionCommand toCommand() {
    return new CreateSubscriptionCommand(tenantId, PlanType.valueOf(plan.toUpperCase()));
  }
}
