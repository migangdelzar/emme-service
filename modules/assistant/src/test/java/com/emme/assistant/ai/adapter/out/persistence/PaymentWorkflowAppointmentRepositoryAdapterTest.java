package com.emme.assistant.ai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.appointment.AppointmentHold;
import com.emme.appointments.application.port.out.AppointmentHoldRepository;
import com.emme.assistant.ai.application.port.out.PaymentWorkflowAppointmentRepository;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.payment.api.port.out.PaymentWorkflowCorrelationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentWorkflowAppointmentRepositoryAdapterTest {

  @Test
  void resolvesTheActiveAppointmentOwnedByThePaymentWorkflow() {
    UUID tenantId = UUID.randomUUID();
    UUID workflowId = UUID.randomUUID();
    UUID holdId = UUID.randomUUID();
    UUID appointmentId = UUID.randomUUID();
    PaymentWorkflowCorrelationRepository correlations =
        mock(PaymentWorkflowCorrelationRepository.class);
    AppointmentHoldRepository holds = mock(AppointmentHoldRepository.class);
    when(correlations.findByWorkflowId(workflowId))
        .thenReturn(
            Optional.of(
                new PaymentWorkflowCorrelationRepository.PaymentWorkflowCorrelation(
                    workflowId, "mock", "provider-1", holdId)));
    when(holds.findById(holdId))
        .thenReturn(
            Optional.of(
                new AppointmentHold(
                    holdId, appointmentId, Instant.parse("2030-01-01T10:00:00Z"), "hold-1")));
    PaymentWorkflowAppointmentRepository adapter =
        new PaymentWorkflowAppointmentRepositoryAdapter(
            correlations,
            holds,
            Clock.fixed(Instant.parse("2030-01-01T09:00:00Z"), ZoneOffset.UTC));

    Optional<UUID> resolved =
        TenantContextHolder.withTenantOverride(
            tenantId, () -> adapter.findAppointmentIdByWorkflowId(workflowId));

    assertThat(resolved).contains(appointmentId);
  }
}
