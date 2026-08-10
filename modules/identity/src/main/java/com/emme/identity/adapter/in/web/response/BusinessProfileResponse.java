package com.emme.identity.adapter.in.web.response;

import java.util.UUID;

/** HTTP representation of the business profile associated with the selected tenant. */
public record BusinessProfileResponse(
    UUID tenantId,
    String businessName,
    String ownerName,
    String monthlyGoal,
    String workingHours,
    String language,
    boolean notificationsEnabled) {}
