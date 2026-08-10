package com.emme.calendar.adapter.in.web.request;

import java.time.Instant;
import java.util.UUID;

/** HTTP request for synchronizing an appointment with a client calendar. */
public record SyncClientCalendarRequest(
    UUID appointmentId, Instant startsAt, Instant endsAt, String summary, String description) {}
