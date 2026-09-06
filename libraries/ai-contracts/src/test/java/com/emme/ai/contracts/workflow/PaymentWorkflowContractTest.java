package com.emme.ai.contracts.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.ai.contracts.appointment.AppointmentHold;
import com.emme.ai.contracts.payment.PaymentLink;
import com.emme.ai.contracts.payment.PaymentWorkflowEvent;
import com.emme.ai.contracts.payment.PaymentWorkflowStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentWorkflowContractTest {

  @Test
  void preservesTrustedWorkflowCorrelationsAndRejectsBlankBoundaryValues() {
    UUID tenantId = UUID.randomUUID();
    UUID workflowId = UUID.randomUUID();
    UUID holdId = UUID.randomUUID();
    Instant expiresAt = Instant.parse("2026-09-05T23:00:00Z");

    AppointmentHold hold =
        new AppointmentHold(holdId, UUID.randomUUID(), expiresAt, "hold-idempotency");
    PaymentLink link =
        new PaymentLink(
            UUID.randomUUID(), workflowId, "mercadopago", "https://checkout.test/1", expiresAt);
    PaymentWorkflowEvent event =
        new PaymentWorkflowEvent(
            tenantId,
            workflowId,
            "mercadopago",
            "event-1",
            "provider-1",
            PaymentWorkflowStatus.CAPTURED);

    assertThat(hold.holdId()).isEqualTo(holdId);
    assertThat(link.workflowId()).isEqualTo(workflowId);
    assertThat(event.tenantId()).isEqualTo(tenantId);
    assertThat(event.status()).isEqualTo(PaymentWorkflowStatus.CAPTURED);
    assertThatThrownBy(
            () ->
                new PaymentWorkflowEvent(
                    tenantId,
                    workflowId,
                    "",
                    "event-1",
                    "provider-1",
                    PaymentWorkflowStatus.CAPTURED))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("provider must not be blank");
  }
}
