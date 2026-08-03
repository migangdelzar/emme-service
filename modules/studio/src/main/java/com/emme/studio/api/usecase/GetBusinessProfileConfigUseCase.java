package com.emme.studio.api.usecase;

import com.emme.studio.api.result.BusinessProfileDetails;
import java.util.Optional;
import java.util.UUID;

/** Retrieves the editable business profile. */
public interface GetBusinessProfileConfigUseCase {

  Optional<BusinessProfileDetails> get(UUID tenantId);
}
