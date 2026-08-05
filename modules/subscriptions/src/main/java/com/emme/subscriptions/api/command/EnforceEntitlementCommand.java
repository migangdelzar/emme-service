package com.emme.subscriptions.api.command;

import java.util.UUID;

public record EnforceEntitlementCommand(UUID tenantId, String entitlement) {}
