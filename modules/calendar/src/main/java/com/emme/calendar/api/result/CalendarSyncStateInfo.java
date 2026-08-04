package com.emme.calendar.api.result;

import java.util.UUID;

/** Public read model returned after a calendar synchronization request. */
public record CalendarSyncStateInfo(UUID id, UUID tenantId, String provider, String status) {}
