package com.emme.identity.adapter.in.web.response;

import java.util.Set;
import java.util.UUID;

/** HTTP representation of a user's membership in a tenant. */
public record TenantMembershipResponse(
    UUID tenantId,
    String tenantSlug,
    String tenantName,
    String displayName,
    String role,
    String status,
    Set<String> permissions) {}
