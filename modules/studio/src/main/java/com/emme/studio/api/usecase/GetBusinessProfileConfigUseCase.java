package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.BusinessProfile;
import java.util.Optional;
import java.util.UUID;

/** Retrieves the editable business profile. */
public interface GetBusinessProfileConfigUseCase {

  Optional<BusinessProfile> get(UUID tenantId);
}
