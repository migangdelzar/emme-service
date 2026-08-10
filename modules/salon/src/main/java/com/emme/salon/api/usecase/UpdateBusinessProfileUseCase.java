package com.emme.salon.api.usecase;

import com.emme.salon.api.result.BusinessProfileDetails;
import java.util.UUID;

/** Updates the editable business profile. */
public interface UpdateBusinessProfileUseCase {

  BusinessProfileDetails update(UUID tenantId, String displayName, String timeZone, String locale);
}
