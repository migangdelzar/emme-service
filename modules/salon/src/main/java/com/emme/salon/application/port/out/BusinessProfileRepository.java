package com.emme.salon.application.port.out;

import com.emme.salon.domain.model.BusinessProfile;
import java.util.Optional;
import java.util.UUID;

/** Persistence capability for tenant business profiles. */
public interface BusinessProfileRepository {

  BusinessProfile save(BusinessProfile profile);

  Optional<BusinessProfile> findByTenantId(UUID tenantId);
}
