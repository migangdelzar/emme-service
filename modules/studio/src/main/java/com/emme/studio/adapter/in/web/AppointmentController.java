package com.emme.studio.adapter.in.web;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.studio.adapter.out.persistence.entity.AppointmentEntity;
import com.emme.studio.application.service.AppointmentService;
import com.emme.studio.application.service.SlotSearchService;
import com.emme.studio.subscriptions.application.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments")
public class AppointmentController {

  private final AppointmentService appointmentService;
  private final SlotSearchService slotSearchService;
  private final SubscriptionService subscriptionService;

  public AppointmentController(
      AppointmentService appointmentService,
      SlotSearchService slotSearchService,
      SubscriptionService subscriptionService) {
    this.appointmentService = appointmentService;
    this.slotSearchService = slotSearchService;
    this.subscriptionService = subscriptionService;
  }

  @GetMapping
  @Operation(summary = "List appointments for current tenant, optionally filtered by date")
  @PreAuthorize("hasRole('platform_admin')")
  public ResponseEntity<List<AppointmentResponse>> list(
      @RequestParam(required = false) LocalDate date) {
    return withCurrentTenant(
        tenantId -> {
          List<AppointmentEntity> appointments;
          if (date != null) {
            appointments = appointmentService.findByTenantAndDate(tenantId, date);
          } else {
            appointments = appointmentService.findByTenantAndDate(tenantId, LocalDate.now());
          }
          return ResponseEntity.ok(appointments.stream().map(AppointmentResponse::from).toList());
        });
  }

  @PostMapping
  @Operation(summary = "Create an appointment (validates collision, returns 409 on conflict)")
  public ResponseEntity<?> create(@Valid @RequestBody CreateAppointmentRequest request) {
    return withCurrentTenant(
        tenantId -> {
          subscriptionService.enforce(tenantId, "appointments:write");
          try {
            AppointmentEntity appointment =
                appointmentService.create(
                    tenantId,
                    request.customerId(),
                    request.serviceId(),
                    request.artistId(),
                    request.startsAt(),
                    request.endsAt());
            var location = URI.create("/api/v1/appointments/" + appointment.getId());
            return ResponseEntity.created(location).body(AppointmentResponse.from(appointment));
          } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
          }
        });
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get appointment by ID")
  public ResponseEntity<AppointmentResponse> get(@PathVariable UUID id) {
    return appointmentService
        .findById(id)
        .map(a -> ResponseEntity.ok(AppointmentResponse.from(a)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{id}/reschedule")
  @Operation(summary = "Reschedule an appointment")
  public ResponseEntity<?> reschedule(
      @PathVariable UUID id, @Valid @RequestBody RescheduleRequest request) {
    try {
      AppointmentEntity appointment =
          appointmentService.reschedule(id, request.newStartsAt(), request.newEndsAt());
      return ResponseEntity.ok(AppointmentResponse.from(appointment));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(409).body(e.getMessage());
    }
  }

  @PostMapping("/{id}/cancel")
  @Operation(summary = "Cancel an appointment")
  public ResponseEntity<AppointmentResponse> cancel(@PathVariable UUID id) {
    return withCurrentTenant(
        tenantId -> {
          subscriptionService.enforce(tenantId, "appointments:write");
          return withConflictHandling(() -> appointmentService.cancel(id));
        });
  }

  @PostMapping("/{id}/confirm")
  @Operation(summary = "Confirm a DRAFT appointment")
  public ResponseEntity<AppointmentResponse> confirm(@PathVariable UUID id) {
    return withConflictHandling(() -> appointmentService.confirm(id));
  }

  @PostMapping("/{id}/start")
  @Operation(summary = "Start a CONFIRMED appointment (-> IN_PROGRESS)")
  public ResponseEntity<AppointmentResponse> start(@PathVariable UUID id) {
    return withConflictHandling(() -> appointmentService.start(id));
  }

  @PostMapping("/{id}/complete")
  @Operation(summary = "Complete an IN_PROGRESS appointment (-> COMPLETED)")
  public ResponseEntity<AppointmentResponse> complete(@PathVariable UUID id) {
    return withConflictHandling(() -> appointmentService.complete(id));
  }

  @PostMapping("/{id}/no-show")
  @Operation(summary = "Mark a CONFIRMED appointment as NO_SHOW")
  public ResponseEntity<AppointmentResponse> noShow(@PathVariable UUID id) {
    return withConflictHandling(() -> appointmentService.noShow(id));
  }

  private ResponseEntity<AppointmentResponse> withConflictHandling(
      Supplier<AppointmentEntity> action) {
    try {
      return ResponseEntity.ok(AppointmentResponse.from(action.get()));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }

  @GetMapping("/slots")
  @Operation(summary = "Find available time slots")
  public ResponseEntity<List<SlotResponse>> findSlots(
      @RequestParam UUID serviceId, @RequestParam LocalDate date) {
    return withCurrentTenant(
        tenantId -> {
          List<SlotSearchService.Slot> slots =
              slotSearchService.findAvailableSlots(tenantId, serviceId, date);
          return ResponseEntity.ok(slots.stream().map(SlotResponse::from).toList());
        });
  }

  // --- DTOs ---

  public record AppointmentResponse(
      UUID id,
      UUID customerId,
      String customerName,
      UUID serviceId,
      String serviceName,
      UUID artistId,
      String artistName,
      Instant startsAt,
      Instant endsAt,
      String status) {
    public static AppointmentResponse from(AppointmentEntity a) {
      return new AppointmentResponse(
          a.getId(),
          a.getCustomer().getId(),
          a.getCustomer().getName(),
          a.getService().getId(),
          a.getService().getName(),
          a.getArtist().getId(),
          a.getArtist().getName(),
          a.getStartsAt(),
          a.getEndsAt(),
          a.getStatus().name());
    }
  }

  public record SlotResponse(UUID artistId, Instant startsAt, Instant endsAt) {
    public static SlotResponse from(SlotSearchService.Slot slot) {
      return new SlotResponse(slot.artistId(), slot.startsAt(), slot.endsAt());
    }
  }

  public record CreateAppointmentRequest(
      @NotNull(message = "customerId is required") UUID customerId,
      @NotNull(message = "serviceId is required") UUID serviceId,
      @NotNull(message = "artistId is required") UUID artistId,
      @NotNull(message = "startsAt is required")
          @FutureOrPresent(message = "startsAt must be in present or future")
          Instant startsAt,
      @NotNull(message = "endsAt is required") @Future(message = "endsAt must be in the future")
          Instant endsAt) {}

  public record RescheduleRequest(
      @NotNull(message = "newStartsAt is required")
          @FutureOrPresent(message = "newStartsAt must be in present or future")
          Instant newStartsAt,
      @NotNull(message = "newEndsAt is required")
          @Future(message = "newEndsAt must be in the future")
          Instant newEndsAt) {}
}
