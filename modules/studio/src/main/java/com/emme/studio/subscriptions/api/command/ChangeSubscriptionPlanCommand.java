package com.emme.studio.subscriptions.api.command;

import com.emme.studio.subscriptions.api.type.PlanType;
import java.util.UUID;

public record ChangeSubscriptionPlanCommand(UUID tenantId, PlanType plan) {}
