package com.emme.salon.application.port.out;

import com.emme.salon.domain.model.BookingPolicy;
import java.util.Optional;
import java.util.UUID;

/** Persistence capability for tenant booking policies. */
public interface BookingPolicyRepository {

  BookingPolicy save(BookingPolicy policy);

  Optional<BookingPolicy> findByTenantId(UUID tenantId);
}
