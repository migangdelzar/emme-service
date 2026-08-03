package com.emme.studio.api.usecase;

import com.emme.studio.api.result.OperatingHoursDetails;
import com.emme.studio.api.type.BusinessDay;
import java.time.LocalTime;
import java.util.UUID;

/** Updates operating hours for one day. */
public interface UpdateOperatingHoursUseCase {

  OperatingHoursDetails update(
      UUID tenantId, BusinessDay day, LocalTime opensAt, LocalTime closesAt, boolean active);
}
