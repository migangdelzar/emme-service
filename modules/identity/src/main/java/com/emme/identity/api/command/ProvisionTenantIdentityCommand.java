package com.emme.identity.api.command;

import java.util.UUID;

/** Requests provisioning of a tenant's Identity-provider realm. */
public record ProvisionTenantIdentityCommand(
    UUID tenantId, String slug, String name, String adminEmail) {}
