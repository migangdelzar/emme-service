package com.emme.e2e.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.client.E2eUserExtension;
import com.emme.client.Role;
import com.emme.client.UserSession;
import com.emme.client.WithUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(E2eUserExtension.class)
@WithUser(role = Role.BUSINESS_OWNER, tenant = "00000000-0000-0000-0000-100000000000")
class CustomerApiTest {

  private static final String DEMO = "00000000-0000-0000-0000-100000000000";

  private UserSession api;

  @org.junit.jupiter.api.BeforeEach
  void setUp(UserSession session) {
    this.api = session;
    session.setup().subscription(DEMO);
  }

  @Test
  void shouldListCustomers(UserSession api) {
    var result = api.customers().list();
    assertThat(result).isNotNull().startsWith("[");
  }

  @Test
  void shouldCreateCustomer(UserSession api) {
    var user = api.user();
    var result = api.customers().create("E2E Test Customer", user.email(), "5550000");
    assertThat(result).isNotNull().contains("\"name\":\"E2E Test Customer\"");
  }

  @Test
  void shouldRejectEmptyName(UserSession api) {
    var result =
        api.post("/api/v1/customers", "{\"name\":\"\",\"email\":\"\",\"phone\":\"\"}", 400);
    assertThat(result).isNotNull();
  }

  @Test
  void shouldSearchCustomers(UserSession api) {
    var result = api.customers().search("e2e");
    assertThat(result).isNotNull();
  }
}
