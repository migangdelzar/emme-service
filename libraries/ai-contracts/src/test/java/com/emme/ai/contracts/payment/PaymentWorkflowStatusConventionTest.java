package com.emme.ai.contracts.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaymentWorkflowStatusConventionTest {
  @Test
  void workflowEventUsesTheContractsOwnedStatusEnum() {
    assertThat(PaymentWorkflowEvent.class.getRecordComponents()[5].getType())
        .isEqualTo(PaymentWorkflowStatus.class);
  }
}
