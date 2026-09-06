package com.emme.assistant.ai.adapter.out.persistence;

import com.emme.appointments.application.port.out.AppointmentHoldRepository;
import com.emme.assistant.ai.application.port.out.PaymentWorkflowAppointmentRepository;
import com.emme.kernel.context.TenantContextHolder;
import com.emme.payment.application.port.out.PaymentWorkflowCorrelationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Resolves the active appointment owned by a tenant-local payment workflow. */
public final class PaymentWorkflowAppointmentRepositoryAdapter
    implements PaymentWorkflowAppointmentRepository {

  private final PaymentWorkflowCorrelationRepository correlations;
  private final AppointmentHoldRepository holds;
  private final Clock clock;

  public PaymentWorkflowAppointmentRepositoryAdapter(
      PaymentWorkflowCorrelationRepository correlations,
      AppointmentHoldRepository holds,
      Clock clock) {
    this.correlations = Objects.requireNonNull(correlations, "correlations must not be null");
    this.holds = Objects.requireNonNull(holds, "holds must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public Optional<UUID> findAppointmentIdByWorkflowId(UUID workflowId) {
    Objects.requireNonNull(workflowId, "workflowId must not be null");
    TenantContextHolder.requireCurrentTenantId();
    Instant now = clock.instant();
    return correlations
        .findByWorkflowId(workflowId)
        .map(PaymentWorkflowCorrelationRepository.PaymentWorkflowCorrelation::appointmentHoldId)
        .flatMap(holds::findById)
        .filter(hold -> hold.expiresAt().isAfter(now))
        .map(com.emme.ai.contracts.appointment.AppointmentHold::appointmentId);
  }
}
