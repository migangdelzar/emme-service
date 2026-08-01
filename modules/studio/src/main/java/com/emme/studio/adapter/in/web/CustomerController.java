package com.emme.studio.adapter.in.web;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.studio.application.service.CustomerService;
import com.emme.studio.domain.model.Customer;
import com.emme.studio.subscriptions.api.command.EnforceEntitlementCommand;
import com.emme.studio.subscriptions.api.usecase.EnforceEntitlementUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers")
public class CustomerController {

  private final CustomerService customerService;
  private final EnforceEntitlementUseCase enforceEntitlement;

  public CustomerController(
      CustomerService customerService, EnforceEntitlementUseCase enforceEntitlement) {
    this.customerService = customerService;
    this.enforceEntitlement = enforceEntitlement;
  }

  @GetMapping
  @Operation(summary = "List customers for current tenant")
  public ResponseEntity<List<CustomerResponse>> list() {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                customerService.findByTenantId(tenantId).stream()
                    .map(CustomerResponse::from)
                    .toList()));
  }

  @PostMapping
  @Operation(summary = "Create a customer")
  public ResponseEntity<CustomerResponse> create(
      @Valid @RequestBody CreateCustomerRequest request) {
    return withCurrentTenant(
        tenantId -> {
          enforceEntitlement.enforce(new EnforceEntitlementCommand(tenantId, "customers:write"));
          Customer customer =
              customerService.create(tenantId, request.name(), request.phone(), request.email());
          var location = URI.create("/api/v1/customers/" + customer.getId());
          return ResponseEntity.created(location).body(CustomerResponse.from(customer));
        });
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get customer by ID")
  public ResponseEntity<CustomerResponse> get(@PathVariable UUID id) {
    return customerService
        .findById(id)
        .map(c -> ResponseEntity.ok(CustomerResponse.from(c)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update a customer")
  public ResponseEntity<CustomerResponse> update(
      @PathVariable UUID id, @Valid @RequestBody UpdateCustomerRequest request) {
    Customer customer =
        customerService.update(id, request.name(), request.phone(), request.email());
    return ResponseEntity.ok(CustomerResponse.from(customer));
  }

  @PostMapping("/{id}/retire")
  @Operation(summary = "Retire a customer")
  public ResponseEntity<CustomerResponse> retire(@PathVariable UUID id) {
    Customer customer = customerService.retire(id);
    return ResponseEntity.ok(CustomerResponse.from(customer));
  }

  @GetMapping("/search")
  @Operation(summary = "Search customers by name")
  public ResponseEntity<List<CustomerResponse>> search(@RequestParam String q) {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                customerService.searchByName(tenantId, q).stream()
                    .map(CustomerResponse::from)
                    .toList()));
  }

  // --- DTOs ---

  public record CustomerResponse(UUID id, String name, String phone, String email, String status) {
    public static CustomerResponse from(Customer c) {
      return new CustomerResponse(
          c.getId(), c.getName(), c.getPhone(), c.getEmail(), c.getStatus().name());
    }
  }

  public record CreateCustomerRequest(@NotBlank String name, String phone, String email) {}

  public record UpdateCustomerRequest(String name, String phone, String email) {}
}
