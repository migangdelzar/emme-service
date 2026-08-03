package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.BookingPolicy;
import java.util.UUID;

/** Updates the booking policy for a tenant. */
public interface UpdateBookingPolicyUseCase {

  BookingPolicy update(
      UUID tenantId, int minNotice, int maxAdvance, int cancelWindow, boolean allowOverlap);
}
