package com.emme.assistant.ai.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** Resolves the appointment owned by a durable payment workflow. */
public interface PaymentWorkflowAppointmentRepository {

  Optional<UUID> findAppointmentIdByWorkflowId(UUID workflowId);
}
