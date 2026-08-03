package com.emme.identity.adapter.in.web.response;

import java.time.Instant;
import java.util.UUID;

/** HTTP representation of a user membership. */
public record MembershipResponse(
    UUID id, UUID tenantId, String role, String userReference, String status, Instant createdAt) {}
