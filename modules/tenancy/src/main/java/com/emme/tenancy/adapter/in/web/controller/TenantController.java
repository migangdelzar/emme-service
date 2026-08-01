package com.emme.tenancy.adapter.in.web.controller;

import com.emme.tenancy.adapter.in.web.mapper.TenantWebMapper;
import com.emme.tenancy.adapter.in.web.request.CreateTenantRequest;
import com.emme.tenancy.adapter.in.web.request.UpdateTenantRequest;
import com.emme.tenancy.adapter.in.web.response.TenantResponse;
import com.emme.tenancy.application.service.TenantService;
import com.emme.tenancy.domain.model.Tenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
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

/** HTTP entry point for the Tenant lifecycle use cases. */
@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenants")
public class TenantController {

  private final TenantService service;
  private final TenantWebMapper mapper;

  public TenantController(TenantService service, TenantWebMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @PostMapping
  @Operation(summary = "Create a new tenant")
  public ResponseEntity<TenantResponse> create(@Valid @RequestBody CreateTenantRequest request) {
    Tenant tenant = service.create(request.slug(), request.name());
    URI location = URI.create("/api/v1/tenants/" + tenant.id());
    return ResponseEntity.created(location).body(mapper.toResponse(tenant));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a tenant by ID")
  public ResponseEntity<TenantResponse> get(@PathVariable UUID id) {
    return service
        .findById(id)
        .map(tenant -> ResponseEntity.ok(mapper.toResponse(tenant)))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping
  @Operation(summary = "List all tenants")
  public ResponseEntity<List<TenantResponse>> list() {
    List<TenantResponse> tenants = service.findAll().stream().map(mapper::toResponse).toList();
    return ResponseEntity.ok(tenants);
  }

  @PatchMapping("/{id}")
  @Operation(summary = "Update a tenant name")
  public ResponseEntity<TenantResponse> update(
      @PathVariable UUID id, @Valid @RequestBody UpdateTenantRequest request) {
    Tenant tenant = service.update(id, request.name());
    return ResponseEntity.ok(mapper.toResponse(tenant));
  }

  @PostMapping("/{id}/suspend")
  @Operation(summary = "Suspend a tenant")
  public ResponseEntity<TenantResponse> suspend(@PathVariable UUID id) {
    Tenant tenant = service.suspend(id);
    return ResponseEntity.ok(mapper.toResponse(tenant));
  }

  @PostMapping("/{id}/reactivate")
  @Operation(summary = "Reactivate a suspended tenant")
  public ResponseEntity<TenantResponse> reactivate(@PathVariable UUID id) {
    Tenant tenant = service.reactivate(id);
    return ResponseEntity.ok(mapper.toResponse(tenant));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Stage-delete a tenant")
  public ResponseEntity<TenantResponse> stageDelete(@PathVariable UUID id) {
    Tenant tenant = service.stageDelete(id);
    return ResponseEntity.ok(mapper.toResponse(tenant));
  }
}
