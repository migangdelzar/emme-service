package com.emme.studio.adapter.in.web.controller;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.studio.adapter.in.web.request.UpdateHoursRequest;
import com.emme.studio.adapter.in.web.request.UpdatePolicyRequest;
import com.emme.studio.adapter.in.web.request.UpdateProfileRequest;
import com.emme.studio.adapter.in.web.response.BookingPolicyResponse;
import com.emme.studio.adapter.in.web.response.BusinessProfileResponse;
import com.emme.studio.adapter.in.web.response.OperatingHoursResponse;
import com.emme.studio.api.result.BookingPolicyDetails;
import com.emme.studio.api.result.BusinessProfileDetails;
import com.emme.studio.api.result.OperatingHoursDetails;
import com.emme.studio.api.usecase.GetBookingPolicyUseCase;
import com.emme.studio.api.usecase.GetBusinessProfileConfigUseCase;
import com.emme.studio.api.usecase.GetOperatingHoursUseCase;
import com.emme.studio.api.usecase.UpdateBookingPolicyUseCase;
import com.emme.studio.api.usecase.UpdateBusinessProfileUseCase;
import com.emme.studio.api.usecase.UpdateOperatingHoursUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/business-config", version = "1.0")
@Tag(name = "Business Config")
public class BusinessConfigurationController {

  private final GetBusinessProfileConfigUseCase getProfile;
  private final UpdateBusinessProfileUseCase updateProfile;
  private final GetOperatingHoursUseCase getHours;
  private final UpdateOperatingHoursUseCase updateHours;
  private final GetBookingPolicyUseCase getPolicy;
  private final UpdateBookingPolicyUseCase updatePolicy;

  public BusinessConfigurationController(
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
          BusinessProfileDetails profile =
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
          OperatingHoursDetails hours =
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
          BookingPolicyDetails policy =
              updatePolicy.update(
                  tenantId,
                  request.minNoticeMinutes(),
                  request.maxAdvanceDays(),
                  request.cancellationWindowMinutes(),
                  request.allowOverlap());
          return ResponseEntity.ok(BookingPolicyResponse.from(policy));
        });
  }
}
