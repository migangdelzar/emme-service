package com.emme.identity.adapter.in.web.controller;

import com.emme.identity.adapter.in.web.mapper.FeatureFlagWebMapper;
import com.emme.identity.adapter.in.web.request.CreateFeatureFlagRequest;
import com.emme.identity.adapter.in.web.request.UpdateFeatureFlagRequest;
import com.emme.identity.adapter.in.web.response.FeatureFlagResponse;
import com.emme.identity.application.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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
  public ResponseEntity<List<FeatureFlagResponse>> listAll() {
    // Platform admin sees all global flags
    // For a complete view, return effective for a dummy tenant (TODO: admin-wide view)
    return ResponseEntity.ok(List.of());
  }

  @PostMapping
  @Operation(summary = "Create a global feature flag")
  public ResponseEntity<FeatureFlagResponse> create(@RequestBody CreateFeatureFlagRequest request) {
    var flag =
        featureFlagService.platformSet(request.code(), request.enabled(), request.planRequired());
    return ResponseEntity.ok(FeatureFlagWebMapper.toResponse(flag));
  }

  @PutMapping("/{code}")
  @Operation(summary = "Update a global feature flag")
  public ResponseEntity<FeatureFlagResponse> update(
      @PathVariable String code, @RequestBody UpdateFeatureFlagRequest request) {
    var flag = featureFlagService.platformSet(code, request.enabled(), request.planRequired());
    return ResponseEntity.ok(FeatureFlagWebMapper.toResponse(flag));
  }
}
