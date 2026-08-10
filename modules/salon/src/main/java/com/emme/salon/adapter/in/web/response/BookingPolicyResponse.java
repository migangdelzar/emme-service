package com.emme.salon.adapter.in.web.response;

import com.emme.salon.api.result.BookingPolicyDetails;
import java.util.UUID;

/** HTTP representation of the tenant booking policy. */
public record BookingPolicyResponse(
    UUID id,
    int minNoticeMinutes,
    int maxAdvanceDays,
    int cancellationWindowMinutes,
    boolean allowOverlap) {

  public static BookingPolicyResponse from(BookingPolicyDetails policy) {
    return new BookingPolicyResponse(
        policy.id(),
        policy.minNoticeMinutes(),
        policy.maxAdvanceDays(),
        policy.cancellationWindowMinutes(),
        policy.allowOverlap());
  }
}
