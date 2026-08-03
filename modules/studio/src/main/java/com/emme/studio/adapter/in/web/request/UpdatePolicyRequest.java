package com.emme.studio.adapter.in.web.request;

/** HTTP request for updating tenant booking-policy values. */
public record UpdatePolicyRequest(
    int minNoticeMinutes,
    int maxAdvanceDays,
    int cancellationWindowMinutes,
    boolean allowOverlap) {}
