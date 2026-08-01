package com.emme.studio.api.usecase;

import com.emme.studio.api.result.BusinessProfileInfo;
import java.util.Optional;
import java.util.UUID;

/** Returns the public business profile for a tenant. */
public interface GetBusinessProfileUseCase {

  Optional<BusinessProfileInfo> getBusinessProfile(UUID tenantId);
}
