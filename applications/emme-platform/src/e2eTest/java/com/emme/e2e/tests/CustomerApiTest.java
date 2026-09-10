package com.emme.e2e.tests;

import static com.emme.client.E2eTest.withSession;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CustomerApiTest {

  @Test
  void shouldListCustomers() {
    withSession(
        s -> {
          s.setup().subscription(s.tenantId());
          var result = s.customers().list();
          assertThat(result).isNotNull().startsWith("[");
        });
  }

  @Test
  void shouldCreateCustomer() {
    withSession(
        s -> {
          s.setup().subscription(s.tenantId());
          var user = s.user();
          var result = s.customers().create("E2E Test Customer", user.email(), "5550000");
          assertThat(result).isNotNull().contains("\"name\":\"E2E Test Customer\"");
        });
  }

  @Test
  void shouldRejectEmptyName() {
    withSession(
        s -> {
          s.setup().subscription(s.tenantId());
          var result =
              s.post("/api/customers", "{\"name\":\"\",\"email\":\"\",\"phone\":\"\"}", 400);
          assertThat(result).isNotNull();
        });
  }

  @Test
  void shouldSearchCustomers() {
    withSession(
        s -> {
          s.setup().subscription(s.tenantId());
          var result = s.customers().search("e2e");
          assertThat(result).isNotNull();
        });
  }
}
