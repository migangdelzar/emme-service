package com.emme.studio.adapter.in.web.request;

import com.emme.studio.api.type.BusinessDay;
import java.time.LocalTime;

/** HTTP request for updating one operating-hours interval. */
public record UpdateHoursRequest(
    BusinessDay day, LocalTime opensAt, LocalTime closesAt, boolean active) {}
