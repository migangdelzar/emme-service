package com.emme.identity.api.result;

import java.util.UUID;

/** Public membership read model returned by Identity use cases. */
public record MembershipInfo(
    UUID id, UUID tenantId, String tenantName, String roleCode, String status) {}
