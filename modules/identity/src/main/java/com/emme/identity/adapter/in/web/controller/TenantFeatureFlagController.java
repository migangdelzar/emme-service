package com.emme.identity.adapter.in.web.controller;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.identity.adapter.in.web.request.OverrideFeatureFlagRequest;
import com.emme.identity.api.command.SetTenantFeatureFlagOverrideCommand;
import com.emme.identity.api.query.GetEffectiveFeatureFlagsQuery;
import com.emme.identity.api.result.FeatureFlagInfo;
import com.emme.identity.api.usecase.GetEffectiveFeatureFlagsUseCase;
import com.emme.identity.api.usecase.SetTenantFeatureFlagOverrideUseCase;
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

  private final GetEffectiveFeatureFlagsUseCase getEffectiveFeatureFlags;
  private final SetTenantFeatureFlagOverrideUseCase setTenantFeatureFlagOverride;

  public TenantFeatureFlagController(
      GetEffectiveFeatureFlagsUseCase getEffectiveFeatureFlags,
      SetTenantFeatureFlagOverrideUseCase setTenantFeatureFlagOverride) {
    this.getEffectiveFeatureFlags = getEffectiveFeatureFlags;
    this.setTenantFeatureFlagOverride = setTenantFeatureFlagOverride;
  }

  @GetMapping
  @Operation(summary = "Get effective feature flags for current tenant")
  public ResponseEntity<Map<String, Boolean>> getEffective() {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                getEffectiveFeatureFlags
                    .get(new GetEffectiveFeatureFlagsQuery(tenantId))
                    .values()));
  }

  @PutMapping("/{code}")
  @Operation(summary = "Set tenant-specific feature flag override")
  public ResponseEntity<Map<String, Object>> setOverride(
      @PathVariable String code, @RequestBody OverrideFeatureFlagRequest request) {
    return withCurrentTenant(
        tenantId -> {
          FeatureFlagInfo featureFlag =
              setTenantFeatureFlagOverride.set(
                  new SetTenantFeatureFlagOverrideCommand(tenantId, code, request.enabled()));
          return ResponseEntity.ok(
              Map.of(
                  "code", featureFlag.code(),
                  "enabled", featureFlag.enabled()));
        });
  }
}
