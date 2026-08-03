package com.emme.studio.adapter.in.web.response;

import com.emme.studio.api.result.OperatingHoursDetails;
import com.emme.studio.api.type.BusinessDay;
import java.time.LocalTime;
import java.util.UUID;

/** HTTP representation of one tenant operating-hours interval. */
public record OperatingHoursResponse(
    UUID id, BusinessDay day, LocalTime opensAt, LocalTime closesAt, boolean active) {

  public static OperatingHoursResponse from(OperatingHoursDetails hours) {
    return new OperatingHoursResponse(
        hours.id(), hours.day(), hours.opensAt(), hours.closesAt(), hours.active());
  }
}
