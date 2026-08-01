package com.emme.tenancy.web;

import com.emme.tenancy.adapter.out.persistence.entity.Tenant;
import com.emme.tenancy.application.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenants")
public class TenantController {

  private final TenantService service;

  public TenantController(TenantService service) {
    this.service = service;
  }

  @PostMapping
  @Operation(summary = "Create a new tenant")
  public ResponseEntity<TenantResponse> create(@Valid @RequestBody CreateTenantRequest request) {
    Tenant tenant = service.create(request.slug(), request.name());
    URI location = URI.create("/api/v1/tenants/" + tenant.getId());
    return ResponseEntity.created(location).body(TenantResponse.from(tenant));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a tenant by ID")
  public ResponseEntity<TenantResponse> get(@PathVariable UUID id) {
    return service
        .findById(id)
        .map(tenant -> ResponseEntity.ok(TenantResponse.from(tenant)))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping
  @Operation(summary = "List all tenants")
  public ResponseEntity<List<TenantResponse>> list() {
    List<TenantResponse> tenants = service.findAll().stream().map(TenantResponse::from).toList();
    return ResponseEntity.ok(tenants);
  }

  @PatchMapping("/{id}")
  @Operation(summary = "Update a tenant name")
  public ResponseEntity<TenantResponse> update(
      @PathVariable UUID id, @Valid @RequestBody UpdateTenantRequest request) {
    Tenant tenant = service.update(id, request.name());
    return ResponseEntity.ok(TenantResponse.from(tenant));
  }

  @PostMapping("/{id}/suspend")
  @Operation(summary = "Suspend a tenant")
  public ResponseEntity<TenantResponse> suspend(@PathVariable UUID id) {
    Tenant tenant = service.suspend(id);
    return ResponseEntity.ok(TenantResponse.from(tenant));
  }

  @PostMapping("/{id}/reactivate")
  @Operation(summary = "Reactivate a suspended tenant")
  public ResponseEntity<TenantResponse> reactivate(@PathVariable UUID id) {
    Tenant tenant = service.reactivate(id);
    return ResponseEntity.ok(TenantResponse.from(tenant));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Stage-delete a tenant")
  public ResponseEntity<TenantResponse> stageDelete(@PathVariable UUID id) {
    Tenant tenant = service.stageDelete(id);
    return ResponseEntity.ok(TenantResponse.from(tenant));
  }

  // --- DTOs ---

  record CreateTenantRequest(@NotBlank String slug, @NotBlank String name) {}

  record UpdateTenantRequest(@NotBlank String name) {}

  record TenantResponse(UUID id, String slug, String name, String status, Instant createdAt) {

    static TenantResponse from(Tenant t) {
      return new TenantResponse(
          t.getId(), t.getSlug(), t.getName(), t.getStatus().name(), t.getCreatedAt());
    }
  }
}
