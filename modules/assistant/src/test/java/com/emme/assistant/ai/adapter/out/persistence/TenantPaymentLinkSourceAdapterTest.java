package com.emme.assistant.ai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.appointment.AppointmentHold;
import com.emme.appointments.application.port.out.AppointmentHoldRepository;
import com.emme.appointments.application.port.out.AppointmentRepository;
import com.emme.appointments.domain.model.Appointment;
import com.emme.assistant.ai.application.port.out.PaymentWorkflowExecutionContextRepository;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.payment.api.port.out.PaymentLinkSourceRepository.PaymentLinkSource;
import com.emme.services.application.port.out.ServiceRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TenantPaymentLinkSourceAdapterTest {

  @Test
  void derivesPaymentFactsFromTheHeldAppointmentAndService() {
    UUID tenantId = UUID.randomUUID();
    UUID workflowId = UUID.randomUUID();
    UUID holdId = UUID.randomUUID();
    UUID appointmentId = UUID.randomUUID();
    UUID serviceId = UUID.randomUUID();
    Instant expiresAt = Instant.parse("2030-01-01T09:15:00Z");
    AppointmentHoldRepository holds = mock(AppointmentHoldRepository.class);
    AppointmentRepository appointments = mock(AppointmentRepository.class);
    ServiceRepository services = mock(ServiceRepository.class);
    PaymentWorkflowExecutionContextRepository workflows =
        mock(PaymentWorkflowExecutionContextRepository.class);
    Appointment appointment =
        Appointment.reconstitute(
            appointmentId,
            tenantId,
            UUID.randomUUID(),
            serviceId,
            UUID.randomUUID(),
            Instant.parse("2030-01-01T10:00:00Z"),
            Instant.parse("2030-01-01T11:00:00Z"),
            com.emme.appointments.domain.model.AppointmentStatus.DRAFT,
            com.emme.appointments.domain.model.ExternalCalendarStatus.NOT_SYNCED);
    com.emme.services.domain.model.Service service =
        com.emme.services.domain.model.Service.reconstitute(
            serviceId,
            tenantId,
            "MANICURE",
            "Manicure",
            "Nails",
            "Classic manicure",
            60,
            new BigDecimal("125.00"),
            com.emme.services.domain.model.ServiceStatus.ACTIVE);
    when(holds.findById(holdId))
        .thenReturn(Optional.of(new AppointmentHold(holdId, appointmentId, expiresAt, "hold-1")));
    when(appointments.findById(appointmentId)).thenReturn(Optional.of(appointment));
    when(services.findById(serviceId)).thenReturn(Optional.of(service));
    when(workflows.findByWorkflowId(workflowId))
        .thenReturn(
            Optional.of(
                new PaymentWorkflowExecutionContextRepository.WorkflowExecutionContext(
                    UUID.randomUUID(), UUID.randomUUID(), "workflow-payment-" + workflowId)));

    PaymentLinkSourceRepositoryAdapter adapter =
        new PaymentLinkSourceRepositoryAdapter(holds, appointments, services, workflows);

    Optional<PaymentLinkSource> source =
        TenantContextHolder.withTenantOverride(
            tenantId, () -> adapter.findByWorkflowIdAndHoldId(workflowId, holdId));

    assertThat(source)
        .contains(new PaymentLinkSource(new BigDecimal("125.00"), "MXN", "Manicure", expiresAt));
  }
}
