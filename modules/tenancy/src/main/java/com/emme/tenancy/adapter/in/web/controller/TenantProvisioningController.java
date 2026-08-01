package com.emme.tenancy.adapter.in.web.controller;

import com.emme.tenancy.adapter.in.web.request.ProvisionTenantRequest;
import com.emme.tenancy.application.service.TenantProvisioningService;
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
@RequestMapping("/api/tenants")
class TenantProvisioningController {

  private final TenantProvisioningService service;

  TenantProvisioningController(TenantProvisioningService service) {
    this.service = service;
  }

  @PostMapping
  ResponseEntity<Map<String, Object>> requestProvisioning(
      @Valid @RequestBody ProvisionTenantRequest request) {
    UUID tenantId =
        service.requestProvisioning(
            request.slug(), request.name(), request.timeZone(), request.locale());
    return ResponseEntity.accepted()
        .location(URI.create("/api/tenants/" + tenantId))
        .body(
            Map.of(
                "tenantId", tenantId,
                "status", "PROVISIONING",
                "message", "Tenant provisioning started"));
  }

  @GetMapping("/{tenantId}")
  ResponseEntity<Map<String, Object>> getStatus(@PathVariable UUID tenantId) {
    var status = service.getStatus(tenantId);
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
