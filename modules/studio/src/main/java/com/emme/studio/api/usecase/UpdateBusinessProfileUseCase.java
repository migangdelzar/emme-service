package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.BusinessProfile;
import java.util.UUID;

/** Updates the editable business profile. */
public interface UpdateBusinessProfileUseCase {

  BusinessProfile update(UUID tenantId, String displayName, String timeZone, String locale);
}
