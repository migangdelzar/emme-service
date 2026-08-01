package com.emme.identity.adapter.in.web.response;

import java.util.List;

/** HTTP representation of the authenticated user and selected business profile. */
public record CurrentUserResponse(
    String userId,
    String email,
    String displayName,
    List<TenantMembershipResponse> memberships,
    BusinessProfileResponse profile) {}
