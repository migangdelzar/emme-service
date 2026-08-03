package com.emme.studio.api.usecase;

import com.emme.studio.domain.model.DayOfWeek;
import com.emme.studio.domain.model.OperatingHours;
import java.time.LocalTime;
import java.util.UUID;

/** Updates operating hours for one day. */
public interface UpdateOperatingHoursUseCase {

  OperatingHours update(
      UUID tenantId, DayOfWeek day, LocalTime opensAt, LocalTime closesAt, boolean active);
}
