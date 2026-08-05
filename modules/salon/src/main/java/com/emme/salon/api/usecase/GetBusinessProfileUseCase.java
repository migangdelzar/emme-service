package com.emme.salon.api.usecase;

import com.emme.salon.api.result.BusinessProfileSummary;
import java.util.Optional;
import java.util.UUID;

/** Returns the public business profile for a tenant. */
public interface GetBusinessProfileUseCase {

  Optional<BusinessProfileSummary> getBusinessProfile(UUID tenantId);
}
