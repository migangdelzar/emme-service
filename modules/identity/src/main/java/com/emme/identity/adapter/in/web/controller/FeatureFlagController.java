package com.emme.identity.adapter.in.web.controller;

import com.emme.identity.adapter.in.web.mapper.FeatureFlagWebMapper;
import com.emme.identity.adapter.in.web.request.CreateFeatureFlagRequest;
import com.emme.identity.adapter.in.web.request.UpdateFeatureFlagRequest;
import com.emme.identity.adapter.in.web.response.FeatureFlagResponse;
import com.emme.identity.api.command.SetPlatformFeatureFlagCommand;
import com.emme.identity.api.result.FeatureFlagInfo;
import com.emme.identity.api.usecase.ListPlatformFeatureFlagsUseCase;
import com.emme.identity.api.usecase.SetPlatformFeatureFlagUseCase;
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
@RequestMapping(path = "/api/admin/feature-flags", version = "1.0")
@Tag(name = "Platform Feature Flags")
@PreAuthorize("hasRole('platform_admin')")
public class FeatureFlagController {

  private final SetPlatformFeatureFlagUseCase setPlatformFeatureFlag;
  private final ListPlatformFeatureFlagsUseCase listPlatformFeatureFlags;

  public FeatureFlagController(
      SetPlatformFeatureFlagUseCase setPlatformFeatureFlag,
      ListPlatformFeatureFlagsUseCase listPlatformFeatureFlags) {
    this.setPlatformFeatureFlag = setPlatformFeatureFlag;
    this.listPlatformFeatureFlags = listPlatformFeatureFlags;
  }

  @GetMapping
  @Operation(summary = "List all global feature flags")
  public ResponseEntity<List<FeatureFlagResponse>> listAll() {
    return ResponseEntity.ok(
        listPlatformFeatureFlags.list().stream().map(FeatureFlagWebMapper::toResponse).toList());
  }

  @PostMapping
  @Operation(summary = "Create a global feature flag")
  public ResponseEntity<FeatureFlagResponse> create(@RequestBody CreateFeatureFlagRequest request) {
    FeatureFlagInfo featureFlag =
        setPlatformFeatureFlag.set(
            new SetPlatformFeatureFlagCommand(
                request.code(), request.enabled(), request.planRequired()));
    return ResponseEntity.ok(FeatureFlagWebMapper.toResponse(featureFlag));
  }

  @PutMapping("/{code}")
  @Operation(summary = "Update a global feature flag")
  public ResponseEntity<FeatureFlagResponse> update(
      @PathVariable String code, @RequestBody UpdateFeatureFlagRequest request) {
    FeatureFlagInfo featureFlag =
        setPlatformFeatureFlag.set(
            new SetPlatformFeatureFlagCommand(code, request.enabled(), request.planRequired()));
    return ResponseEntity.ok(FeatureFlagWebMapper.toResponse(featureFlag));
  }
}
