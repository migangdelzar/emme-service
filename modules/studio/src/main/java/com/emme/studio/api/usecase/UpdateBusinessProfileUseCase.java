package com.emme.studio.api.usecase;

import com.emme.studio.api.result.BusinessProfileDetails;
import java.util.UUID;

/** Updates the editable business profile. */
public interface UpdateBusinessProfileUseCase {

  BusinessProfileDetails update(UUID tenantId, String displayName, String timeZone, String locale);
}
