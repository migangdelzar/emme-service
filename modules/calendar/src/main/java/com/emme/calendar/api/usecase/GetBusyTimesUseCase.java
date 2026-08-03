package com.emme.calendar.api.usecase;

import com.emme.calendar.api.result.CalendarBusyTimeRange;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Reads external calendar busy intervals for an artist and date. */
public interface GetBusyTimesUseCase {

  List<CalendarBusyTimeRange> getBusyTimes(UUID tenantId, UUID artistId, LocalDate date);
}
