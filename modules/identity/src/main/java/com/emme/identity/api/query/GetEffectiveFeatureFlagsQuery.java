package com.emme.identity.api.query;

import java.util.UUID;

/** Public query for the effective feature-flag values of a tenant. */
public record GetEffectiveFeatureFlagsQuery(UUID tenantId) {}
