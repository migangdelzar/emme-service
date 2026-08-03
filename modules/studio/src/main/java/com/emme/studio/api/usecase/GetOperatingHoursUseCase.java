package com.emme.studio.api.usecase;

import com.emme.studio.api.result.OperatingHoursDetails;
import java.util.List;
import java.util.UUID;

/** Retrieves operating hours for a tenant. */
public interface GetOperatingHoursUseCase {

  List<OperatingHoursDetails> get(UUID tenantId);
}
