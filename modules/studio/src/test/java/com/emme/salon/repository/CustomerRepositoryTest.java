package com.emme.studio.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.studio.adapter.out.persistence.entity.CustomerEntity;
import com.emme.studio.adapter.out.persistence.repository.SpringDataCustomerRepository;
import com.emme.studio.domain.model.CustomerStatus;
import com.emme.testing.BaseRepositoryTest;
import com.emme.testing.TestSecurityConfig;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(TestSecurityConfig.class)
class CustomerRepositoryTest extends BaseRepositoryTest {

  @Autowired private SpringDataCustomerRepository customerRepo;

  private static final UUID TENANT_ID = UUID.randomUUID();

  @Test
  void shouldSaveAndFindCustomer() {
    CustomerEntity customer = new CustomerEntity(TENANT_ID, "Repo Customer");
    customer.setEmail("repo@example.com");
    customer.setPhone("555-0001");
    CustomerEntity saved = customerRepo.save(customer);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getStatus()).isEqualTo(CustomerStatus.ACTIVE);

    CustomerEntity found = customerRepo.findById(saved.getId()).orElseThrow();
    assertThat(found.getName()).isEqualTo("Repo Customer");
    assertThat(found.getEmail()).isEqualTo("repo@example.com");
  }

  @Test
  void shouldFindByEmail() {
    CustomerEntity customer = new CustomerEntity(TENANT_ID, "Email Customer");
    customer.setEmail("unique@example.com");
    customerRepo.save(customer);

    Optional<CustomerEntity> found =
        customerRepo.findByTenantIdAndEmail(TENANT_ID, "unique@example.com");
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Email Customer");
  }
}
