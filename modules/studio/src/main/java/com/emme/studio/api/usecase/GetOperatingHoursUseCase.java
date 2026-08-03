package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.OperatingHours;
import java.util.List;
import java.util.UUID;

/** Retrieves operating hours for a tenant. */
public interface GetOperatingHoursUseCase {

  List<OperatingHours> get(UUID tenantId);
}
