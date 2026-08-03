package com.emme.studio.adapter.in.web;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.studio.api.usecase.GetBookingPolicyUseCase;
import com.emme.studio.api.usecase.GetBusinessProfileConfigUseCase;
import com.emme.studio.api.usecase.GetOperatingHoursUseCase;
import com.emme.studio.api.usecase.UpdateBookingPolicyUseCase;
import com.emme.studio.api.usecase.UpdateBusinessProfileUseCase;
import com.emme.studio.api.usecase.UpdateOperatingHoursUseCase;
import com.emme.studio.domain.model.BookingPolicy;
import com.emme.studio.domain.model.BusinessProfile;
import com.emme.studio.domain.model.DayOfWeek;
import com.emme.studio.domain.model.OperatingHours;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/business-config")
@Tag(name = "Business Config")
public class BusinessConfigController {

  private final GetBusinessProfileConfigUseCase getProfile;
  private final UpdateBusinessProfileUseCase updateProfile;
  private final GetOperatingHoursUseCase getHours;
  private final UpdateOperatingHoursUseCase updateHours;
  private final GetBookingPolicyUseCase getPolicy;
  private final UpdateBookingPolicyUseCase updatePolicy;

  public BusinessConfigController(
      GetBusinessProfileConfigUseCase getProfile,
      UpdateBusinessProfileUseCase updateProfile,
      GetOperatingHoursUseCase getHours,
      UpdateOperatingHoursUseCase updateHours,
      GetBookingPolicyUseCase getPolicy,
      UpdateBookingPolicyUseCase updatePolicy) {
    this.getProfile = getProfile;
    this.updateProfile = updateProfile;
    this.getHours = getHours;
    this.updateHours = updateHours;
    this.getPolicy = getPolicy;
    this.updatePolicy = updatePolicy;
  }

  // --- Profile ---

  @GetMapping("/profile")
  @Operation(summary = "Get business profile")
  public ResponseEntity<BusinessProfileResponse> getProfile() {
    return withCurrentTenant(
        tenantId ->
            getProfile
                .get(tenantId)
                .map(p -> ResponseEntity.ok(BusinessProfileResponse.from(p)))
                .orElse(ResponseEntity.notFound().build()));
  }

  @PutMapping("/profile")
  @Operation(summary = "Update business profile")
  public ResponseEntity<BusinessProfileResponse> updateProfile(
      @RequestBody UpdateProfileRequest request) {
    return withCurrentTenant(
        tenantId -> {
          BusinessProfile profile =
              updateProfile.update(
                  tenantId, request.displayName(), request.timeZone(), request.locale());
          return ResponseEntity.ok(BusinessProfileResponse.from(profile));
        });
  }

  // --- Operating Hours ---

  @GetMapping("/hours")
  @Operation(summary = "Get operating hours")
  public ResponseEntity<List<OperatingHoursResponse>> getHours() {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                getHours.get(tenantId).stream().map(OperatingHoursResponse::from).toList()));
  }

  @PutMapping("/hours")
  @Operation(summary = "Update operating hours for a day")
  public ResponseEntity<OperatingHoursResponse> updateHours(
      @RequestBody UpdateHoursRequest request) {
    return withCurrentTenant(
        tenantId -> {
          OperatingHours hours =
              updateHours.update(
                  tenantId, request.day(), request.opensAt(), request.closesAt(), request.active());
          return ResponseEntity.ok(OperatingHoursResponse.from(hours));
        });
  }

  // --- Booking Policy ---

  @GetMapping("/policy")
  @Operation(summary = "Get booking policy")
  public ResponseEntity<BookingPolicyResponse> getPolicy() {
    return withCurrentTenant(
        tenantId ->
            getPolicy
                .get(tenantId)
                .map(p -> ResponseEntity.ok(BookingPolicyResponse.from(p)))
                .orElse(ResponseEntity.notFound().build()));
  }

  @PutMapping("/policy")
  @Operation(summary = "Update booking policy")
  public ResponseEntity<BookingPolicyResponse> updatePolicy(
      @RequestBody UpdatePolicyRequest request) {
    return withCurrentTenant(
        tenantId -> {
          BookingPolicy policy =
              updatePolicy.update(
                  tenantId,
                  request.minNoticeMinutes(),
                  request.maxAdvanceDays(),
                  request.cancellationWindowMinutes(),
                  request.allowOverlap());
          return ResponseEntity.ok(BookingPolicyResponse.from(policy));
        });
  }

  // --- DTOs ---

  public record BusinessProfileResponse(
      UUID id, String displayName, String timeZone, String locale) {
    public static BusinessProfileResponse from(BusinessProfile p) {
      return new BusinessProfileResponse(
          p.getId(), p.getDisplayName(), p.getTimeZone(), p.getLocale());
    }
  }

  public record OperatingHoursResponse(
      UUID id, DayOfWeek dayOfWeek, LocalTime opensAt, LocalTime closesAt, boolean active) {
    public static OperatingHoursResponse from(OperatingHours oh) {
      return new OperatingHoursResponse(
          oh.getId(), oh.getDayOfWeek(), oh.getOpensAt(), oh.getClosesAt(), oh.isActive());
    }
  }

  public record BookingPolicyResponse(
      UUID id,
      int minNoticeMinutes,
      int maxAdvanceDays,
      int cancellationWindowMinutes,
      boolean allowOverlap) {
    public static BookingPolicyResponse from(BookingPolicy bp) {
      return new BookingPolicyResponse(
          bp.getId(),
          bp.getMinNoticeMinutes(),
          bp.getMaxAdvanceDays(),
          bp.getCancellationWindowMinutes(),
          bp.isAllowOverlap());
    }
  }

  public record UpdateProfileRequest(String displayName, String timeZone, String locale) {}

  public record UpdateHoursRequest(
      DayOfWeek day, LocalTime opensAt, LocalTime closesAt, boolean active) {}

  public record UpdatePolicyRequest(
      int minNoticeMinutes,
      int maxAdvanceDays,
      int cancellationWindowMinutes,
      boolean allowOverlap) {}
}
