package com.emme.payment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentTest {

  @Test
  void transitionsPaymentThroughItsSupportedLifecycle() {
    Payment payment = new Payment(UUID.randomUUID(), "provider-1", BigDecimal.TEN, "MXN");

    payment.authorize();
    payment.capture();
    payment.refund();

    assertThat(payment.status()).isEqualTo(PaymentStatus.REFUNDED);
  }

  @Test
  void rejectsCaptureBeforeAuthorization() {
    Payment payment = new Payment(UUID.randomUUID(), "provider-1", BigDecimal.TEN, "MXN");

    assertThatThrownBy(payment::capture)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Cannot capture payment in PENDING");
  }
}
