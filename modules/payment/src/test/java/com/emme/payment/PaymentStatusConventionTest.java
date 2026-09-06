package com.emme.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.payment.adapter.in.web.response.PaymentResponse;
import com.emme.payment.api.type.PaymentStatus;
import org.junit.jupiter.api.Test;

class PaymentStatusConventionTest {
  @Test
  void paymentResponseUsesThePublicStatusEnum() {
    assertThat(PaymentResponse.class.getRecordComponents()[4].getType())
        .isEqualTo(PaymentStatus.class);
  }
}
