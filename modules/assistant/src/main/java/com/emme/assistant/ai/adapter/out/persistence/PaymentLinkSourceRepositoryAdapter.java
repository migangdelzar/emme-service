package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.ai.contracts.appointment.AppointmentHold;
import com.emme.appointments.application.port.out.AppointmentHoldRepository;
import com.emme.appointments.application.port.out.AppointmentRepository;
import com.emme.assistant.ai.application.port.out.PaymentWorkflowExecutionContextRepository;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.payment.application.port.out.PaymentLinkSourceRepository;
import com.emme.services.application.port.out.ServiceRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Derives payment facts from tenant-routed appointment state. */
@Component
public final class PaymentLinkSourceRepositoryAdapter implements PaymentLinkSourceRepository {

  private static final String DEFAULT_CURRENCY = "MXN";

  private final AppointmentHoldRepository holds;
  private final AppointmentRepository appointments;
  private final ServiceRepository services;
  private final PaymentWorkflowExecutionContextRepository workflows;

  public PaymentLinkSourceRepositoryAdapter(
      AppointmentHoldRepository holds,
      AppointmentRepository appointments,
      ServiceRepository services,
      PaymentWorkflowExecutionContextRepository workflows) {
    this.holds = Objects.requireNonNull(holds, "holds must not be null");
    this.appointments = Objects.requireNonNull(appointments, "appointments must not be null");
    this.services = Objects.requireNonNull(services, "services must not be null");
    this.workflows = Objects.requireNonNull(workflows, "workflows must not be null");
  }

  @Override
  public Optional<PaymentLinkSource> findByWorkflowIdAndHoldId(UUID workflowId, UUID holdId) {
    Objects.requireNonNull(workflowId, "workflowId must not be null");
    Objects.requireNonNull(holdId, "holdId must not be null");
    UUID tenantId = TenantContextHolder.requireCurrentTenantId();

    return workflows
        .findByWorkflowId(workflowId)
        .flatMap(ignored -> holds.findById(holdId))
        .flatMap(hold -> sourceForHold(hold, tenantId));
  }

  private Optional<PaymentLinkSource> sourceForHold(AppointmentHold hold, UUID tenantId) {
    return appointments
        .findById(hold.appointmentId())
        .flatMap(
            appointment -> {
              if (!tenantId.equals(appointment.getTenantId())) {
                throw new SecurityException("Appointment does not belong to the current tenant");
              }
              return services.findById(appointment.getServiceId());
            })
        .map(
            service ->
                new PaymentLinkSource(
                    service.getBasePrice(), DEFAULT_CURRENCY, service.getName(), hold.expiresAt()));
  }
}
