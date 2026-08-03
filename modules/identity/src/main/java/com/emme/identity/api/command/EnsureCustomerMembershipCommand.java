package com.emme.identity.api.command;

import java.util.UUID;

/** Public intent to ensure a customer's membership exists for a tenant. */
public record EnsureCustomerMembershipCommand(UUID customerId, UUID tenantId) {}
