package com.emme.identity.adapter.in.web.controller;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.identity.adapter.in.web.request.OverrideFeatureFlagRequest;
import com.emme.identity.application.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenant/features")
@Tag(name = "Tenant Features")
public class TenantFeatureFlagController {

  private final FeatureFlagService featureFlagService;

  public TenantFeatureFlagController(FeatureFlagService featureFlagService) {
    this.featureFlagService = featureFlagService;
  }

  @GetMapping
  @Operation(summary = "Get effective feature flags for current tenant")
  public ResponseEntity<Map<String, Boolean>> getEffective() {
    return withCurrentTenant(
        tenantId -> ResponseEntity.ok(featureFlagService.getEffective(tenantId)));
  }

  @PutMapping("/{code}")
  @Operation(summary = "Set tenant-specific feature flag override")
  public ResponseEntity<Map<String, Object>> setOverride(
      @PathVariable String code, @RequestBody OverrideFeatureFlagRequest request) {
    return withCurrentTenant(
        tenantId -> {
          var flag = featureFlagService.setOverride(tenantId, code, request.enabled());
          return ResponseEntity.ok(
              Map.of(
                  "code", flag.code(),
                  "enabled", flag.isEnabled()));
        });
  }
}
