package com.emme.subscriptions.api.command;

import com.emme.subscriptions.api.type.PlanType;
import java.util.UUID;

public record CreateSubscriptionCommand(UUID tenantId, PlanType plan) {}
