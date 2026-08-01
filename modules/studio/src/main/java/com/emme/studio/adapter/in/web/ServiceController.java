package com.emme.studio.adapter.in.web;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.studio.application.service.ServiceCatalogService;
import com.emme.studio.domain.model.Service;
import com.emme.studio.subscriptions.api.command.EnforceEntitlementCommand;
import com.emme.studio.subscriptions.api.usecase.EnforceEntitlementUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "Services")
public class ServiceController {

  private final ServiceCatalogService serviceCatalogService;
  private final EnforceEntitlementUseCase enforceEntitlement;

  public ServiceController(
      ServiceCatalogService serviceCatalogService, EnforceEntitlementUseCase enforceEntitlement) {
    this.serviceCatalogService = serviceCatalogService;
    this.enforceEntitlement = enforceEntitlement;
  }

  @GetMapping
  @Operation(summary = "List active services for current tenant")
  public ResponseEntity<List<ServiceResponse>> list() {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                serviceCatalogService.findActiveServices(tenantId).stream()
                    .map(ServiceResponse::from)
                    .toList()));
  }

  @PostMapping
  @Operation(summary = "Create a service")
  public ResponseEntity<ServiceResponse> create(@Valid @RequestBody CreateServiceRequest request) {
    return withCurrentTenant(
        tenantId -> {
          enforceEntitlement.enforce(new EnforceEntitlementCommand(tenantId, "services:write"));
          Service service =
              request.category() == null && request.description() == null
                  ? serviceCatalogService.create(
                      tenantId,
                      request.code(),
                      request.name(),
                      request.durationMinutes(),
                      request.basePrice())
                  : serviceCatalogService.create(
                      tenantId,
                      request.code(),
                      request.name(),
                      request.category(),
                      request.description(),
                      request.durationMinutes(),
                      request.basePrice());
          var location = URI.create("/api/v1/services/" + service.getId());
          return ResponseEntity.created(location).body(ServiceResponse.from(service));
        });
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get service by ID")
  public ResponseEntity<ServiceResponse> get(@PathVariable UUID id) {
    return serviceCatalogService
        .findById(id)
        .map(s -> ResponseEntity.ok(ServiceResponse.from(s)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update a service")
  public ResponseEntity<ServiceResponse> update(
      @PathVariable UUID id, @Valid @RequestBody UpdateServiceRequest request) {
    Service service =
        request.category() == null && request.description() == null
            ? serviceCatalogService.update(
                id, request.name(), request.durationMinutes(), request.basePrice())
            : serviceCatalogService.update(
                id,
                request.name(),
                request.category(),
                request.description(),
                request.durationMinutes(),
                request.basePrice());
    return ResponseEntity.ok(ServiceResponse.from(service));
  }

  @PostMapping("/{id}/retire")
  @Operation(summary = "Retire a service")
  public ResponseEntity<ServiceResponse> retire(@PathVariable UUID id) {
    Service service = serviceCatalogService.retire(id);
    return ResponseEntity.ok(ServiceResponse.from(service));
  }

  // --- DTOs ---

  public record ServiceResponse(
      UUID id,
      String code,
      String name,
      String category,
      String description,
      int durationMinutes,
      BigDecimal basePrice,
      String status) {
    public static ServiceResponse from(Service s) {
      return new ServiceResponse(
          s.getId(),
          s.getCode(),
          s.getName(),
          s.getCategory(),
          s.getDescription(),
          s.getDurationMinutes(),
          s.getBasePrice(),
          s.getStatus().name());
    }
  }

  public record CreateServiceRequest(
      String code,
      String name,
      String category,
      String description,
      int durationMinutes,
      @NotNull BigDecimal basePrice) {}

  public record UpdateServiceRequest(
      String name,
      String category,
      String description,
      int durationMinutes,
      BigDecimal basePrice) {}
}
