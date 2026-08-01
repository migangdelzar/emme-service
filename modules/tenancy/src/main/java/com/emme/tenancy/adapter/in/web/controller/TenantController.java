package com.emme.tenancy.adapter.in.web.controller;

import com.emme.tenancy.adapter.in.web.mapper.TenantWebMapper;
import com.emme.tenancy.adapter.in.web.request.CreateTenantRequest;
import com.emme.tenancy.adapter.in.web.request.UpdateTenantRequest;
import com.emme.tenancy.adapter.in.web.response.TenantResponse;
import com.emme.tenancy.api.command.CreateTenantCommand;
import com.emme.tenancy.api.command.ReactivateTenantCommand;
import com.emme.tenancy.api.command.StageDeleteTenantCommand;
import com.emme.tenancy.api.command.SuspendTenantCommand;
import com.emme.tenancy.api.command.UpdateTenantCommand;
import com.emme.tenancy.api.query.GetTenantQuery;
import com.emme.tenancy.api.query.ListTenantsQuery;
import com.emme.tenancy.api.result.TenantInfo;
import com.emme.tenancy.api.usecase.CreateTenantUseCase;
import com.emme.tenancy.api.usecase.GetTenantUseCase;
import com.emme.tenancy.api.usecase.ListTenantsUseCase;
import com.emme.tenancy.api.usecase.ReactivateTenantUseCase;
import com.emme.tenancy.api.usecase.StageDeleteTenantUseCase;
import com.emme.tenancy.api.usecase.SuspendTenantUseCase;
import com.emme.tenancy.api.usecase.UpdateTenantUseCase;
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

  private final CreateTenantUseCase createTenant;
  private final GetTenantUseCase getTenant;
  private final ListTenantsUseCase listTenants;
  private final UpdateTenantUseCase updateTenant;
  private final SuspendTenantUseCase suspendTenant;
  private final ReactivateTenantUseCase reactivateTenant;
  private final StageDeleteTenantUseCase stageDeleteTenant;
  private final TenantWebMapper mapper;

  public TenantController(
      CreateTenantUseCase createTenant,
      GetTenantUseCase getTenant,
      ListTenantsUseCase listTenants,
      UpdateTenantUseCase updateTenant,
      SuspendTenantUseCase suspendTenant,
      ReactivateTenantUseCase reactivateTenant,
      StageDeleteTenantUseCase stageDeleteTenant,
      TenantWebMapper mapper) {
    this.createTenant = createTenant;
    this.getTenant = getTenant;
    this.listTenants = listTenants;
    this.updateTenant = updateTenant;
    this.suspendTenant = suspendTenant;
    this.reactivateTenant = reactivateTenant;
    this.stageDeleteTenant = stageDeleteTenant;
    this.mapper = mapper;
  }

  @PostMapping
  @Operation(summary = "Create a new tenant")
  public ResponseEntity<TenantResponse> create(@Valid @RequestBody CreateTenantRequest request) {
    TenantInfo tenant =
        createTenant.create(new CreateTenantCommand(request.slug(), request.name()));
    URI location = URI.create("/api/v1/tenants/" + tenant.id());
    return ResponseEntity.created(location).body(mapper.toResponse(tenant));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a tenant by ID")
  public ResponseEntity<TenantResponse> get(@PathVariable UUID id) {
    return getTenant
        .get(new GetTenantQuery(id))
        .map(tenant -> ResponseEntity.ok(mapper.toResponse(tenant)))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping
  @Operation(summary = "List all tenants")
  public ResponseEntity<List<TenantResponse>> list() {
    List<TenantResponse> tenants =
        listTenants.list(new ListTenantsQuery()).stream().map(mapper::toResponse).toList();
    return ResponseEntity.ok(tenants);
  }

  @PatchMapping("/{id}")
  @Operation(summary = "Update a tenant name")
  public ResponseEntity<TenantResponse> update(
      @PathVariable UUID id, @Valid @RequestBody UpdateTenantRequest request) {
    TenantInfo tenant = updateTenant.update(new UpdateTenantCommand(id, request.name()));
    return ResponseEntity.ok(mapper.toResponse(tenant));
  }

  @PostMapping("/{id}/suspend")
  @Operation(summary = "Suspend a tenant")
  public ResponseEntity<TenantResponse> suspend(@PathVariable UUID id) {
    TenantInfo tenant = suspendTenant.suspend(new SuspendTenantCommand(id));
    return ResponseEntity.ok(mapper.toResponse(tenant));
  }

  @PostMapping("/{id}/reactivate")
  @Operation(summary = "Reactivate a suspended tenant")
  public ResponseEntity<TenantResponse> reactivate(@PathVariable UUID id) {
    TenantInfo tenant = reactivateTenant.reactivate(new ReactivateTenantCommand(id));
    return ResponseEntity.ok(mapper.toResponse(tenant));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Stage-delete a tenant")
  public ResponseEntity<TenantResponse> stageDelete(@PathVariable UUID id) {
    TenantInfo tenant = stageDeleteTenant.stageDelete(new StageDeleteTenantCommand(id));
    return ResponseEntity.ok(mapper.toResponse(tenant));
  }
}
