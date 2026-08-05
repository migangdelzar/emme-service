package com.emme.clients.adapter.in.web.controller;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.clients.adapter.in.web.request.CreateCustomerRequest;
import com.emme.clients.adapter.in.web.request.UpdateCustomerRequest;
import com.emme.clients.adapter.in.web.response.CustomerResponse;
import com.emme.clients.api.result.CustomerDetails;
import com.emme.clients.api.usecase.CreateCustomerUseCase;
import com.emme.clients.api.usecase.GetCustomerUseCase;
import com.emme.clients.api.usecase.ListTenantCustomersUseCase;
import com.emme.clients.api.usecase.RetireCustomerUseCase;
import com.emme.clients.api.usecase.SearchCustomersUseCase;
import com.emme.clients.api.usecase.UpdateCustomerUseCase;
import com.emme.subscriptions.api.command.EnforceEntitlementCommand;
import com.emme.subscriptions.api.usecase.EnforceEntitlementUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping(path = "/api/customers", version = "1.0")
@Tag(name = "Customers")
public class CustomerController {

  private final ListTenantCustomersUseCase listCustomers;
  private final CreateCustomerUseCase createCustomer;
  private final GetCustomerUseCase getCustomer;
  private final UpdateCustomerUseCase updateCustomer;
  private final RetireCustomerUseCase retireCustomer;
  private final SearchCustomersUseCase searchCustomers;
  private final EnforceEntitlementUseCase enforceEntitlement;

  public CustomerController(
      ListTenantCustomersUseCase listCustomers,
      CreateCustomerUseCase createCustomer,
      GetCustomerUseCase getCustomer,
      UpdateCustomerUseCase updateCustomer,
      RetireCustomerUseCase retireCustomer,
      SearchCustomersUseCase searchCustomers,
      EnforceEntitlementUseCase enforceEntitlement) {
    this.listCustomers = listCustomers;
    this.createCustomer = createCustomer;
    this.getCustomer = getCustomer;
    this.updateCustomer = updateCustomer;
    this.retireCustomer = retireCustomer;
    this.searchCustomers = searchCustomers;
    this.enforceEntitlement = enforceEntitlement;
  }

  @GetMapping
  @Operation(summary = "List customers for current tenant")
  public ResponseEntity<List<CustomerResponse>> list() {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                listCustomers.list(tenantId).stream().map(CustomerResponse::from).toList()));
  }

  @PostMapping
  @Operation(summary = "Create a customer")
  public ResponseEntity<CustomerResponse> create(
      @Valid @RequestBody CreateCustomerRequest request) {
    return withCurrentTenant(
        tenantId -> {
          enforceEntitlement.enforce(new EnforceEntitlementCommand(tenantId, "customers:write"));
          CustomerDetails customer =
              createCustomer.create(tenantId, request.name(), request.phone(), request.email());
          var location = URI.create("/api/customers/" + customer.id());
          return ResponseEntity.created(location).body(CustomerResponse.from(customer));
        });
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get customer by ID")
  public ResponseEntity<CustomerResponse> get(@PathVariable UUID id) {
    return getCustomer
        .get(id)
        .map(c -> ResponseEntity.ok(CustomerResponse.from(c)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update a customer")
  public ResponseEntity<CustomerResponse> update(
      @PathVariable UUID id, @Valid @RequestBody UpdateCustomerRequest request) {
    CustomerDetails customer =
        updateCustomer.update(id, request.name(), request.phone(), request.email());
    return ResponseEntity.ok(CustomerResponse.from(customer));
  }

  @PostMapping("/{id}/retire")
  @Operation(summary = "Retire a customer")
  public ResponseEntity<CustomerResponse> retire(@PathVariable UUID id) {
    CustomerDetails customer = retireCustomer.retire(id);
    return ResponseEntity.ok(CustomerResponse.from(customer));
  }

  @GetMapping("/search")
  @Operation(summary = "Search customers by name")
  public ResponseEntity<List<CustomerResponse>> search(@RequestParam String q) {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                searchCustomers.search(tenantId, q).stream().map(CustomerResponse::from).toList()));
  }
}
