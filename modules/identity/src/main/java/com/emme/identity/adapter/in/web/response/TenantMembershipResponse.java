package com.emme.identity.adapter.in.web.response;

import com.emme.identity.domain.model.MembershipStatus;
import java.util.Set;
import java.util.UUID;

/** HTTP representation of a user's membership in a tenant. */
public record TenantMembershipResponse(
    UUID tenantId,
    String tenantSlug,
    String tenantName,
    String displayName,
    String role,
    MembershipStatus status,
    Set<String> permissions) {}
