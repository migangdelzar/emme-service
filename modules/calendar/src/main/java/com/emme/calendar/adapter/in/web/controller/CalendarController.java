package com.emme.calendar.adapter.in.web.controller;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.calendar.adapter.in.web.response.CalendarBusyTimeResponse;
import com.emme.calendar.adapter.in.web.response.CalendarSyncStateResponse;
import com.emme.calendar.api.result.CalendarBusyTimeRange;
import com.emme.calendar.api.usecase.GetBusyTimesUseCase;
import com.emme.calendar.api.usecase.SyncCalendarEventsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/calendar", version = "1.0")
@Tag(name = "Calendar")
public class CalendarController {

  private final GetBusyTimesUseCase getBusyTimes;
  private final SyncCalendarEventsUseCase syncCalendarEvents;

  public CalendarController(
      GetBusyTimesUseCase getBusyTimes, SyncCalendarEventsUseCase syncCalendarEvents) {
    this.getBusyTimes = getBusyTimes;
    this.syncCalendarEvents = syncCalendarEvents;
  }

  @GetMapping("/busy")
  @Operation(summary = "Get busy times for an artist on a given date")
  public ResponseEntity<List<CalendarBusyTimeResponse>> getBusyTimes(
      @RequestParam UUID artistId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return withCurrentTenant(
        tenantId -> {
          List<CalendarBusyTimeRange> busyTimes =
              getBusyTimes.getBusyTimes(tenantId, artistId, date);
          return ResponseEntity.ok(busyTimes.stream().map(CalendarBusyTimeResponse::from).toList());
        });
  }

  @PostMapping("/sync")
  @Operation(summary = "Trigger calendar sync for current tenant")
  @PreAuthorize("@featureFlagService.isEnabled('calendar_sync')")
  public ResponseEntity<CalendarSyncStateResponse> sync() {
    return withCurrentTenant(
        tenantId -> {
          var state = syncCalendarEvents.sync(tenantId);
          return ResponseEntity.ok(CalendarSyncStateResponse.from(state));
        });
  }
}
