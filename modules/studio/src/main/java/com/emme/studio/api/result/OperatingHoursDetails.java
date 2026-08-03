package com.emme.studio.api.result;

import com.emme.studio.api.type.BusinessDay;
import java.time.LocalTime;
import java.util.UUID;

/** Stable public operating-hours representation. */
public record OperatingHoursDetails(
    UUID id, BusinessDay day, LocalTime opensAt, LocalTime closesAt, boolean active) {}
