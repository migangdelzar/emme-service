package com.emme.clients.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.clients.application.port.out.CustomerRepository;
import com.emme.clients.domain.model.Customer;
import com.emme.kernel.context.TenantContextHolder;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerTenantScopeTest {

  @Test
  void getsCustomerThroughTheCurrentTenant() {
    CustomerRepository repository = org.mockito.Mockito.mock();
    GetCustomerService service = new GetCustomerService(repository);
    UUID tenantId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    Customer customer =
        Customer.reconstitute(
            customerId,
            tenantId,
            "Customer",
            null,
            null,
            com.emme.clients.domain.model.CustomerStatus.ACTIVE);
    when(repository.findByTenantIdAndId(tenantId, customerId)).thenReturn(Optional.of(customer));

    var result = TenantContextHolder.withTenantOverride(tenantId, () -> service.get(customerId));

    verify(repository).findByTenantIdAndId(tenantId, customerId);
    assertThat(result).isPresent();
  }

  @Test
  void updatesCustomerThroughTheCurrentTenant() {
    CustomerRepository repository = org.mockito.Mockito.mock();
    UpdateCustomerService service = new UpdateCustomerService(repository);
    UUID tenantId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    Customer customer =
        Customer.reconstitute(
            customerId,
            tenantId,
            "Before",
            null,
            null,
            com.emme.clients.domain.model.CustomerStatus.ACTIVE);
    when(repository.findByTenantIdAndId(tenantId, customerId)).thenReturn(Optional.of(customer));
    when(repository.save(customer)).thenReturn(customer);

    TenantContextHolder.withTenantOverride(
        tenantId, () -> service.update(customerId, "After", null, null));

    verify(repository).findByTenantIdAndId(tenantId, customerId);
    assertThat(customer.getName()).isEqualTo("After");
  }

  @Test
  void retiresCustomerThroughTheCurrentTenant() {
    CustomerRepository repository = org.mockito.Mockito.mock();
    RetireCustomerService service = new RetireCustomerService(repository);
    UUID tenantId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    Customer customer =
        Customer.reconstitute(
            customerId,
            tenantId,
            "Customer",
            null,
            null,
            com.emme.clients.domain.model.CustomerStatus.ACTIVE);
    when(repository.findByTenantIdAndId(tenantId, customerId)).thenReturn(Optional.of(customer));
    when(repository.save(customer)).thenReturn(customer);

    TenantContextHolder.withTenantOverride(tenantId, () -> service.retire(customerId));

    verify(repository).findByTenantIdAndId(tenantId, customerId);
    assertThat(customer.getStatus())
        .isEqualTo(com.emme.clients.domain.model.CustomerStatus.RETIRED);
  }
}
