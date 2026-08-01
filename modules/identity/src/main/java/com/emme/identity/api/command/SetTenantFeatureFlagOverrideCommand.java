package com.emme.identity.api.command;

import java.util.UUID;

/** Public intent to create or update a tenant-specific feature-flag override. */
public record SetTenantFeatureFlagOverrideCommand(UUID tenantId, String code, boolean enabled) {}
