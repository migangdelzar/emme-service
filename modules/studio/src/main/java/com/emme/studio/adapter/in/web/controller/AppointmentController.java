package com.emme.studio.adapter.in.web.controller;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.studio.api.result.AppointmentDetails;
import com.emme.studio.api.result.AvailableSlot;
import com.emme.studio.api.usecase.CancelAppointmentUseCase;
import com.emme.studio.api.usecase.CompleteAppointmentUseCase;
import com.emme.studio.api.usecase.ConfirmAppointmentUseCase;
import com.emme.studio.api.usecase.CreateAppointmentUseCase;
import com.emme.studio.api.usecase.FindAvailableSlotsUseCase;
import com.emme.studio.api.usecase.GetAppointmentUseCase;
import com.emme.studio.api.usecase.ListAppointmentsByDateUseCase;
import com.emme.studio.api.usecase.MarkAppointmentNoShowUseCase;
import com.emme.studio.api.usecase.RescheduleAppointmentUseCase;
import com.emme.studio.api.usecase.StartAppointmentUseCase;
import com.emme.studio.subscriptions.api.command.EnforceEntitlementCommand;
import com.emme.studio.subscriptions.api.usecase.EnforceEntitlementUseCase;
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
@RequestMapping("/api/appointments")
@Tag(name = "Appointments")
public class AppointmentController {

  private final ListAppointmentsByDateUseCase listAppointments;
  private final CreateAppointmentUseCase createAppointment;
  private final GetAppointmentUseCase getAppointment;
  private final RescheduleAppointmentUseCase rescheduleAppointment;
  private final CancelAppointmentUseCase cancelAppointment;
  private final ConfirmAppointmentUseCase confirmAppointment;
  private final StartAppointmentUseCase startAppointment;
  private final CompleteAppointmentUseCase completeAppointment;
  private final MarkAppointmentNoShowUseCase markAppointmentNoShow;
  private final FindAvailableSlotsUseCase findAvailableSlots;
  private final EnforceEntitlementUseCase enforceEntitlement;

  public AppointmentController(
      ListAppointmentsByDateUseCase listAppointments,
      CreateAppointmentUseCase createAppointment,
      GetAppointmentUseCase getAppointment,
      RescheduleAppointmentUseCase rescheduleAppointment,
      CancelAppointmentUseCase cancelAppointment,
      ConfirmAppointmentUseCase confirmAppointment,
      StartAppointmentUseCase startAppointment,
      CompleteAppointmentUseCase completeAppointment,
      MarkAppointmentNoShowUseCase markAppointmentNoShow,
      FindAvailableSlotsUseCase findAvailableSlots,
      EnforceEntitlementUseCase enforceEntitlement) {
    this.listAppointments = listAppointments;
    this.createAppointment = createAppointment;
    this.getAppointment = getAppointment;
    this.rescheduleAppointment = rescheduleAppointment;
    this.cancelAppointment = cancelAppointment;
    this.confirmAppointment = confirmAppointment;
    this.startAppointment = startAppointment;
    this.completeAppointment = completeAppointment;
    this.markAppointmentNoShow = markAppointmentNoShow;
    this.findAvailableSlots = findAvailableSlots;
    this.enforceEntitlement = enforceEntitlement;
  }

  @GetMapping
  @Operation(summary = "List appointments for current tenant, optionally filtered by date")
  @PreAuthorize("hasRole('platform_admin')")
  public ResponseEntity<List<AppointmentResponse>> list(
      @RequestParam(required = false) LocalDate date) {
    return withCurrentTenant(
        tenantId -> {
          List<AppointmentDetails> appointments;
          if (date != null) {
            appointments = listAppointments.list(tenantId, date);
          } else {
            appointments = listAppointments.list(tenantId, LocalDate.now());
          }
          return ResponseEntity.ok(appointments.stream().map(AppointmentResponse::from).toList());
        });
  }

  @PostMapping
  @Operation(summary = "Create an appointment (validates collision, returns 409 on conflict)")
  public ResponseEntity<?> create(@Valid @RequestBody CreateAppointmentRequest request) {
    return withCurrentTenant(
        tenantId -> {
          enforceEntitlement.enforce(new EnforceEntitlementCommand(tenantId, "appointments:write"));
          try {
            AppointmentDetails appointment =
                createAppointment.create(
                    tenantId,
                    request.customerId(),
                    request.serviceId(),
                    request.artistId(),
                    request.startsAt(),
                    request.endsAt());
            var location = URI.create("/api/appointments/" + appointment.id());
            return ResponseEntity.created(location).body(AppointmentResponse.from(appointment));
          } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
          }
        });
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get appointment by ID")
  public ResponseEntity<AppointmentResponse> get(@PathVariable UUID id) {
    return getAppointment
        .get(id)
        .map(a -> ResponseEntity.ok(AppointmentResponse.from(a)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PutMapping("/{id}/reschedule")
  @Operation(summary = "Reschedule an appointment")
  public ResponseEntity<?> reschedule(
      @PathVariable UUID id, @Valid @RequestBody RescheduleRequest request) {
    try {
      AppointmentDetails appointment =
          rescheduleAppointment.reschedule(id, request.newStartsAt(), request.newEndsAt());
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
          enforceEntitlement.enforce(new EnforceEntitlementCommand(tenantId, "appointments:write"));
          return withConflictHandling(() -> cancelAppointment.cancel(id));
        });
  }

  @PostMapping("/{id}/confirm")
  @Operation(summary = "Confirm a DRAFT appointment")
  public ResponseEntity<AppointmentResponse> confirm(@PathVariable UUID id) {
    return withConflictHandling(() -> confirmAppointment.confirm(id));
  }

  @PostMapping("/{id}/start")
  @Operation(summary = "Start a CONFIRMED appointment (-> IN_PROGRESS)")
  public ResponseEntity<AppointmentResponse> start(@PathVariable UUID id) {
    return withConflictHandling(() -> startAppointment.start(id));
  }

  @PostMapping("/{id}/complete")
  @Operation(summary = "Complete an IN_PROGRESS appointment (-> COMPLETED)")
  public ResponseEntity<AppointmentResponse> complete(@PathVariable UUID id) {
    return withConflictHandling(() -> completeAppointment.complete(id));
  }

  @PostMapping("/{id}/no-show")
  @Operation(summary = "Mark a CONFIRMED appointment as NO_SHOW")
  public ResponseEntity<AppointmentResponse> noShow(@PathVariable UUID id) {
    return withConflictHandling(() -> markAppointmentNoShow.markNoShow(id));
  }

  private ResponseEntity<AppointmentResponse> withConflictHandling(
      Supplier<AppointmentDetails> action) {
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
          List<AvailableSlot> slots = findAvailableSlots.find(tenantId, serviceId, date);
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
    public static AppointmentResponse from(AppointmentDetails a) {
      return new AppointmentResponse(
          a.id(),
          a.customerId(),
          a.customerName(),
          a.serviceId(),
          a.serviceName(),
          a.artistId(),
          a.artistName(),
          a.startsAt(),
          a.endsAt(),
          a.status());
    }
  }

  public record SlotResponse(UUID artistId, Instant startsAt, Instant endsAt) {
    public static SlotResponse from(AvailableSlot slot) {
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
