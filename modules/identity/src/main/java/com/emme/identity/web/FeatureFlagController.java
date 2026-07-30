package com.emme.identity.web;

import com.emme.identity.application.FeatureFlagService;
import com.emme.studio.subscriptions.api.PlanType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/feature-flags")
@Tag(name = "Platform Feature Flags")
@PreAuthorize("hasRole('platform_admin')")
public class FeatureFlagController {

  private final FeatureFlagService featureFlagService;

  public FeatureFlagController(FeatureFlagService featureFlagService) {
    this.featureFlagService = featureFlagService;
  }

  @GetMapping
  @Operation(summary = "List all global feature flags")
  public ResponseEntity<List<FlagResponse>> listAll() {
    // Platform admin sees all global flags
    // For a complete view, return effective for a dummy tenant (TODO: admin-wide view)
    return ResponseEntity.ok(List.of());
  }

  @PostMapping
  @Operation(summary = "Create a global feature flag")
  public ResponseEntity<FlagResponse> create(@RequestBody CreateFlagRequest request) {
    var flag =
        featureFlagService.platformSet(request.code(), request.enabled(), request.planRequired());
    return ResponseEntity.ok(FlagResponse.from(flag));
  }

  @PutMapping("/{code}")
  @Operation(summary = "Update a global feature flag")
  public ResponseEntity<FlagResponse> update(
      @PathVariable String code, @RequestBody UpdateFlagRequest request) {
    var flag = featureFlagService.platformSet(code, request.enabled(), request.planRequired());
    return ResponseEntity.ok(FlagResponse.from(flag));
  }

  // --- DTOs ---

  public record CreateFlagRequest(String code, boolean enabled, PlanType planRequired) {}

  public record UpdateFlagRequest(boolean enabled, PlanType planRequired) {}

  public record FlagResponse(
      UUID id, String code, boolean enabled, PlanType planRequired, String description) {
    public static FlagResponse from(com.emme.identity.entity.FeatureFlag f) {
      return new FlagResponse(
          f.getId(), f.getCode(), f.isEnabled(), f.getPlanRequired(), f.getDescription());
    }
  }
}
