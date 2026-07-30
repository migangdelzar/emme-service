package com.emme.identity.api;

import java.util.UUID;

public record MembershipInfo(
    UUID id, UUID tenantId, String tenantName, String roleCode, String status) {}
