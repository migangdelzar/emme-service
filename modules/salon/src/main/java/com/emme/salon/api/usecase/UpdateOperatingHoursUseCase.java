package com.emme.salon.api.usecase;

import com.emme.salon.api.result.OperatingHoursDetails;
import com.emme.salon.api.type.BusinessDay;
import java.time.LocalTime;
import java.util.UUID;

/** Updates operating hours for one day. */
public interface UpdateOperatingHoursUseCase {

  OperatingHoursDetails update(
      UUID tenantId, BusinessDay day, LocalTime opensAt, LocalTime closesAt, boolean active);
}
