package com.emme.calendar.api.result;

/** Public result of a client-calendar synchronization operation. */
public record ClientCalendarSyncDetails(String status, String eventId) {}
