package com.emme.studio.application.port.out;

import com.emme.studio.domain.model.BookingPolicy;
import java.util.Optional;
import java.util.UUID;

/** Persistence capability for tenant booking policies. */
public interface BookingPolicyRepository {

  BookingPolicy save(BookingPolicy policy);

  Optional<BookingPolicy> findByTenantId(UUID tenantId);
}
