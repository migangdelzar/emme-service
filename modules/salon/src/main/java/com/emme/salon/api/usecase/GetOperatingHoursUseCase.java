package com.emme.salon.api.usecase;

import com.emme.salon.api.result.OperatingHoursDetails;
import java.util.List;
import java.util.UUID;

/** Retrieves operating hours for a tenant. */
public interface GetOperatingHoursUseCase {

  List<OperatingHoursDetails> get(UUID tenantId);
}
