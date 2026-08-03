package com.emme.studio.api.result;

import java.util.UUID;

/** Stable public booking-policy representation. */
public record BookingPolicyDetails(
    UUID id,
    int minNoticeMinutes,
    int maxAdvanceDays,
    int cancellationWindowMinutes,
    boolean allowOverlap) {}
