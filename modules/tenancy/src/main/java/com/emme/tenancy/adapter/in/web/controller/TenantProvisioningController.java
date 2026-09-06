package com.emme.tenancy.adapter.in.web.controller;

import com.emme.tenancy.adapter.in.web.request.ProvisionTenantRequest;
import com.emme.tenancy.api.command.RequestTenantProvisioningCommand;
import com.emme.tenancy.api.query.GetTenantProvisioningStatusQuery;
import com.emme.tenancy.api.result.TenantProvisioningStatus;
import com.emme.tenancy.api.usecase.GetTenantProvisioningStatusUseCase;
import com.emme.tenancy.api.usecase.RequestTenantProvisioningUseCase;
import com.emme.tenancy.domain.model.TenantProvisioningState;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP entry point for asynchronous tenant provisioning requests and status. */
@RestController
@RequestMapping(path = "/api/tenant-provisioning", version = "1.0")
class TenantProvisioningController {

  private final RequestTenantProvisioningUseCase requestProvisioning;
  private final GetTenantProvisioningStatusUseCase getProvisioningStatus;

  TenantProvisioningController(
      RequestTenantProvisioningUseCase requestProvisioning,
      GetTenantProvisioningStatusUseCase getProvisioningStatus) {
    this.requestProvisioning = requestProvisioning;
    this.getProvisioningStatus = getProvisioningStatus;
  }

  @PostMapping
  ResponseEntity<Map<String, Object>> requestProvisioning(
      @Valid @RequestBody ProvisionTenantRequest request) {
    UUID tenantId =
        requestProvisioning.request(
            new RequestTenantProvisioningCommand(
                request.slug(), request.name(), request.timeZone(), request.locale()));
    return ResponseEntity.accepted()
        .location(URI.create("/api/tenant-provisioning/" + tenantId))
        .body(
            Map.of(
                "tenantId",
                tenantId,
                "status",
                TenantProvisioningState.PROVISIONING,
                "message",
                "Tenant provisioning started"));
  }

  @GetMapping("/{tenantId}")
  ResponseEntity<Map<String, Object>> getStatus(@PathVariable UUID tenantId) {
    TenantProvisioningStatus status =
        getProvisioningStatus.get(new GetTenantProvisioningStatusQuery(tenantId));
    return ResponseEntity.ok(
        Map.of(
            "tenantId",
            tenantId,
            "status",
            status.status(),
            "schemaName",
            status.schemaName(),
            "lastMigratedAt",
            status.lastMigratedAt() != null ? status.lastMigratedAt().toString() : null,
            "error",
            status.error() != null ? status.error() : ""));
  }
}
