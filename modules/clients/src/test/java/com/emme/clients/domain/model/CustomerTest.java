package com.emme.clients.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerTest {

  @Test
  void newCustomerStartsActiveWithoutPersistenceIdentity() {
    Customer customer = new Customer(UUID.randomUUID(), "Ada");

    assertThat(customer.getId()).isNull();
    assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
  }

  @Test
  void retiringCustomerChangesOnlyItsBusinessStatus() {
    UUID tenantId = UUID.randomUUID();
    Customer customer = new Customer(tenantId, "Ada");
    customer.setPhone("555-0100");

    customer.retire();

    assertThat(customer.getTenantId()).isEqualTo(tenantId);
    assertThat(customer.getPhone()).isEqualTo("555-0100");
    assertThat(customer.getStatus()).isEqualTo(CustomerStatus.RETIRED);
  }
}
