package com.emme.calendar.adapter.in.web.controller;

import com.emme.calendar.adapter.in.web.request.SyncClientCalendarRequest;
import com.emme.calendar.adapter.in.web.response.ClientCalendarSyncResponse;
import com.emme.calendar.api.command.SyncClientCalendarCommand;
import com.emme.calendar.api.command.UnsyncClientCalendarCommand;
import com.emme.calendar.api.usecase.SyncClientCalendarUseCase;
import com.emme.calendar.api.usecase.UnsyncClientCalendarUseCase;
import com.emme.identity.adapter.in.web.security.UserContextHolder;
import com.emme.kernel.context.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping(path = "/api/client/calendar", version = "1.0")
@Tag(name = "Client Calendar")
public class ClientCalendarController {

  private final SyncClientCalendarUseCase syncClientCalendar;
  private final UnsyncClientCalendarUseCase unsyncClientCalendar;

  public ClientCalendarController(
      SyncClientCalendarUseCase syncClientCalendar,
      UnsyncClientCalendarUseCase unsyncClientCalendar) {
    this.syncClientCalendar = syncClientCalendar;
    this.unsyncClientCalendar = unsyncClientCalendar;
  }

  @PostMapping("/sync")
  @PreAuthorize(
      "@featureFlagService.isEnabled('google_workspace') and @featureFlagService.isEnabled('client_google_sync')")
  @Operation(summary = "Sync a client appointment to their Google Calendar")
  public ResponseEntity<ClientCalendarSyncResponse> sync(
      @RequestBody SyncClientCalendarRequest request) {
    UUID tenantId = TenantContextHolder.requireCurrentTenantId();
    var details =
        syncClientCalendar.sync(
            new SyncClientCalendarCommand(
                tenantId,
                request.appointmentId(),
                UserContextHolder.currentSubject(),
                request.startsAt(),
                request.endsAt(),
                request.summary(),
                request.description()));
    return ResponseEntity.ok(ClientCalendarSyncResponse.from(details));
  }

  @DeleteMapping("/sync/{appointmentId}")
  @PreAuthorize(
      "@featureFlagService.isEnabled('google_workspace') and @featureFlagService.isEnabled('client_google_sync')")
  @Operation(summary = "Unsync appointment from Google Calendar")
  public ResponseEntity<Void> unsync(@PathVariable UUID appointmentId) {
    UUID tenantId = TenantContextHolder.requireCurrentTenantId();
    unsyncClientCalendar.unsync(
        new UnsyncClientCalendarCommand(
            tenantId, appointmentId, UserContextHolder.currentSubject()));
    return ResponseEntity.noContent().build();
  }
}
