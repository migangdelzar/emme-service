package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.BookingPolicy;
import java.util.Optional;
import java.util.UUID;

/** Retrieves the booking policy for a tenant. */
public interface GetBookingPolicyUseCase {

  Optional<BookingPolicy> get(UUID tenantId);
}
