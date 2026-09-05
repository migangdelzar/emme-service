package com.emme.clients.adapter.out.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.clients.adapter.out.persistence.entity.CustomerEntity;
import com.emme.clients.adapter.out.persistence.repository.SpringDataCustomerRepository;
import com.emme.clients.domain.model.Customer;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerPersistenceAdapterTest {

  @Test
  void updatesAnExistingCustomerByIdWithinTheTenantScopedConnection() {
    SpringDataCustomerRepository repository = org.mockito.Mockito.mock();
    CustomerPersistenceAdapter adapter = new CustomerPersistenceAdapter(repository);
    UUID tenantId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    CustomerEntity entity = new CustomerEntity(tenantId, "Before");
    Customer customer =
        Customer.reconstitute(
            customerId,
            tenantId,
            "After",
            "555",
            null,
            com.emme.clients.domain.model.CustomerStatus.ACTIVE);
    when(repository.findById(customerId)).thenReturn(Optional.of(entity));
    when(repository.save(entity)).thenReturn(entity);

    Customer saved = adapter.save(customer);

    verify(repository).findById(customerId);
    assertThat(entity.getName()).isEqualTo("After");
    assertThat(saved.getTenantId()).isEqualTo(tenantId);
    assertThat(saved.getName()).isEqualTo("After");
  }
}
