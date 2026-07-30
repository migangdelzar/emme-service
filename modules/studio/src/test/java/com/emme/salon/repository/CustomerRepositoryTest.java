package com.emme.studio.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.studio.entity.Customer;
import com.emme.studio.entity.CustomerRepository;
import com.emme.studio.entity.CustomerStatus;
import com.emme.testing.BaseRepositoryTest;
import com.emme.testing.TestSecurityConfig;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(TestSecurityConfig.class)
class CustomerRepositoryTest extends BaseRepositoryTest {

  @Autowired private CustomerRepository customerRepo;

  private static final UUID TENANT_ID = UUID.randomUUID();

  @Test
  void shouldSaveAndFindCustomer() {
    Customer customer = new Customer(TENANT_ID, "Repo Customer");
    customer.setEmail("repo@example.com");
    customer.setPhone("555-0001");
    Customer saved = customerRepo.save(customer);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getStatus()).isEqualTo(CustomerStatus.ACTIVE);

    Customer found = customerRepo.findById(saved.getId()).orElseThrow();
    assertThat(found.getName()).isEqualTo("Repo Customer");
    assertThat(found.getEmail()).isEqualTo("repo@example.com");
  }

  @Test
  void shouldFindByEmail() {
    Customer customer = new Customer(TENANT_ID, "Email Customer");
    customer.setEmail("unique@example.com");
    customerRepo.save(customer);

    Optional<Customer> found = customerRepo.findByTenantIdAndEmail(TENANT_ID, "unique@example.com");
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Email Customer");
  }
}
