package com.emme.clients.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.clients.application.port.out.CustomerRepository;
import com.emme.clients.domain.model.Customer;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerTenantScopeTest {

  @Test
  void getsCustomerByIdFromTheTenantScopedConnection() {
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
    when(repository.findById(customerId)).thenReturn(Optional.of(customer));

    var result = service.get(customerId);

    verify(repository).findById(customerId);
    assertThat(result).isPresent();
  }

  @Test
  void updatesCustomerByIdFromTheTenantScopedConnection() {
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
    when(repository.findById(customerId)).thenReturn(Optional.of(customer));
    when(repository.save(customer)).thenReturn(customer);

    service.update(customerId, "After", null, null);

    verify(repository).findById(customerId);
    assertThat(customer.getName()).isEqualTo("After");
  }

  @Test
  void retiresCustomerByIdFromTheTenantScopedConnection() {
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
    when(repository.findById(customerId)).thenReturn(Optional.of(customer));
    when(repository.save(customer)).thenReturn(customer);

    service.retire(customerId);

    verify(repository).findById(customerId);
    assertThat(customer.getStatus())
        .isEqualTo(com.emme.clients.domain.model.CustomerStatus.RETIRED);
  }
}
