package com.emme.calendar.infrastructure.google.web;

import com.emme.calendar.infrastructure.google.application.ClientCalendarSyncService;
import com.emme.kernel.context.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/client/calendar")
@Tag(name = "Client Calendar")
public class ClientCalendarController {

  private final ClientCalendarSyncService syncService;

  public ClientCalendarController(ClientCalendarSyncService syncService) {
    this.syncService = syncService;
  }

  @PostMapping("/sync")
  @PreAuthorize(
      "@featureFlagService.isEnabled('google_workspace') and @featureFlagService.isEnabled('client_google_sync')")
  @Operation(summary = "Sync a client appointment to their Google Calendar")
  public ResponseEntity<Object> sync(@RequestBody SyncRequest request) {
    UUID tenantId = TenantContextHolder.requireCurrentTenantId();
    String eventId =
        syncService.syncAppointment(
            tenantId,
            request.appointmentId(),
            request.startsAt(),
            request.endsAt(),
            request.summary(),
            request.description());
    return ResponseEntity.ok(Map.of("status", "synced", "eventId", eventId));
  }

  @DeleteMapping("/sync/{appointmentId}")
  @PreAuthorize(
      "@featureFlagService.isEnabled('google_workspace') and @featureFlagService.isEnabled('client_google_sync')")
  @Operation(summary = "Unsync appointment from Google Calendar")
  public ResponseEntity<Void> unsync(@PathVariable UUID appointmentId) {
    UUID tenantId = TenantContextHolder.requireCurrentTenantId();
    syncService.unsyncAppointment(tenantId, appointmentId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Request body for syncing an appointment to a client's Google Calendar.
   *
   * @param appointmentId the appointment to sync
   * @param startsAt appointment start time (UTC)
   * @param endsAt appointment end time (UTC)
   * @param summary event title / summary
   * @param description optional event description
   */
  public record SyncRequest(
      UUID appointmentId, Instant startsAt, Instant endsAt, String summary, String description) {}
}
