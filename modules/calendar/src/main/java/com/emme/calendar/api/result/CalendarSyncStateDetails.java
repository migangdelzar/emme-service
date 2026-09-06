package com.emme.calendar.api.result;

import com.emme.calendar.domain.model.CalendarSyncStatus;
import java.util.UUID;

/** Public read model returned after a calendar synchronization request. */
public record CalendarSyncStateDetails(
    UUID id, UUID tenantId, String provider, CalendarSyncStatus status) {}
