package com.emme.e2e.tests;

import static com.emme.client.E2eTest.withSession;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaymentApiTest {

  @Test
  void shouldListPayments() {
    withSession(
        s -> {
          var result = s.payments().list();
          assertThat(result).isNotNull().startsWith("[");
        });
  }

  @Test
  void shouldCreatePayment() {
    withSession(
        s -> {
          var result = s.payments().create("e2e-test-ref-001", "100.00", "MXN");
          assertThat(result).isNotNull().contains("\"status\"");
        });
  }
}
