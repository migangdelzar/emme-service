package com.emme.studio.api.usecase;

import com.emme.studio.api.result.BookingPolicyDetails;
import java.util.UUID;

/** Updates the booking policy for a tenant. */
public interface UpdateBookingPolicyUseCase {

  BookingPolicyDetails update(
      UUID tenantId, int minNotice, int maxAdvance, int cancelWindow, boolean allowOverlap);
}
