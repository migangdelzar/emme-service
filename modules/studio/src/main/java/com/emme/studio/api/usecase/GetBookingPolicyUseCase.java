package com.emme.studio.api.usecase;

import com.emme.studio.api.result.BookingPolicyDetails;
import java.util.Optional;
import java.util.UUID;

/** Retrieves the booking policy for a tenant. */
public interface GetBookingPolicyUseCase {

  Optional<BookingPolicyDetails> get(UUID tenantId);
}
