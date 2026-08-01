package com.emme.studio.adapter.in.web;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.studio.adapter.out.persistence.entity.BookingPolicyEntity;
import com.emme.studio.adapter.out.persistence.entity.BusinessProfileEntity;
import com.emme.studio.adapter.out.persistence.entity.OperatingHoursEntity;
import com.emme.studio.application.service.BusinessConfigService;
import com.emme.studio.domain.model.DayOfWeek;
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
@RequestMapping("/api/v1/business-config")
@Tag(name = "Business Config")
public class BusinessConfigController {

  private final BusinessConfigService businessConfigService;

  public BusinessConfigController(BusinessConfigService businessConfigService) {
    this.businessConfigService = businessConfigService;
  }

  // --- Profile ---

  @GetMapping("/profile")
  @Operation(summary = "Get business profile")
  public ResponseEntity<BusinessProfileResponse> getProfile() {
    return withCurrentTenant(
        tenantId ->
            businessConfigService
                .getProfile(tenantId)
                .map(p -> ResponseEntity.ok(BusinessProfileResponse.from(p)))
                .orElse(ResponseEntity.notFound().build()));
  }

  @PutMapping("/profile")
  @Operation(summary = "Update business profile")
  public ResponseEntity<BusinessProfileResponse> updateProfile(
      @RequestBody UpdateProfileRequest request) {
    return withCurrentTenant(
        tenantId -> {
          BusinessProfileEntity profile =
              businessConfigService.updateProfile(
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
                businessConfigService.getOperatingHours(tenantId).stream()
                    .map(OperatingHoursResponse::from)
                    .toList()));
  }

  @PutMapping("/hours")
  @Operation(summary = "Update operating hours for a day")
  public ResponseEntity<OperatingHoursResponse> updateHours(
      @RequestBody UpdateHoursRequest request) {
    return withCurrentTenant(
        tenantId -> {
          OperatingHoursEntity hours =
              businessConfigService.updateOperatingHours(
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
            businessConfigService
                .getBookingPolicy(tenantId)
                .map(p -> ResponseEntity.ok(BookingPolicyResponse.from(p)))
                .orElse(ResponseEntity.notFound().build()));
  }

  @PutMapping("/policy")
  @Operation(summary = "Update booking policy")
  public ResponseEntity<BookingPolicyResponse> updatePolicy(
      @RequestBody UpdatePolicyRequest request) {
    return withCurrentTenant(
        tenantId -> {
          BookingPolicyEntity policy =
              businessConfigService.updateBookingPolicy(
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
    public static BusinessProfileResponse from(BusinessProfileEntity p) {
      return new BusinessProfileResponse(
          p.getId(), p.getDisplayName(), p.getTimeZone(), p.getLocale());
    }
  }

  public record OperatingHoursResponse(
      UUID id, DayOfWeek dayOfWeek, LocalTime opensAt, LocalTime closesAt, boolean active) {
    public static OperatingHoursResponse from(OperatingHoursEntity oh) {
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
    public static BookingPolicyResponse from(BookingPolicyEntity bp) {
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
