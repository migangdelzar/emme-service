package com.emme.calendar.api.command;

import java.util.UUID;

/** Requests removal of a client appointment event from Google Calendar. */
public record UnsyncClientCalendarCommand(UUID tenantId, UUID appointmentId, String userId) {}
