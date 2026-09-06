package com.emme.assistant.ai.adapter.out.workflow;

import com.emme.ai.contracts.payment.PaymentWorkflowEvent;
import com.emme.ai.contracts.workflow.PaymentWorkflow;
import com.emme.ai.contracts.workflow.WorkflowHandle;
import com.emme.ai.contracts.workflow.WorkflowStatus;
import com.emme.appointments.api.usecase.ConfirmAppointmentUseCase;
import com.emme.assistant.ai.application.port.out.PaymentWorkflowAppointmentRepository;
import com.emme.kernel.context.AiExecutionContext;
import java.util.Objects;
import java.util.UUID;

/** Projects verified payment events into durable booking workflow lifecycle states. */
public final class AppointmentPaymentWorkflow implements PaymentWorkflow {

  private final ConfirmAppointmentUseCase confirmations;
  private final PaymentWorkflowAppointmentRepository appointments;

  public AppointmentPaymentWorkflow(
      ConfirmAppointmentUseCase confirmations, PaymentWorkflowAppointmentRepository appointments) {
    this.confirmations = Objects.requireNonNull(confirmations, "confirmations must not be null");
    this.appointments = Objects.requireNonNull(appointments, "appointments must not be null");
  }

  @Override
  public WorkflowHandle resume(PaymentWorkflowEvent event, AiExecutionContext context) {
    Objects.requireNonNull(event, "event must not be null");
    Objects.requireNonNull(context, "context must not be null");
    if (!context.workflowId().equals(event.workflowId())) {
      throw new SecurityException("workflowId does not match AI execution context");
    }
    return switch (event.status()) {
      case "PENDING", "AUTHORIZED" ->
          new WorkflowHandle(context.workflowId(), WorkflowStatus.WAITING_FOR_PAYMENT, 0);
      case "CAPTURED" -> {
        UUID appointmentId =
            appointments
                .findAppointmentIdByWorkflowId(context.workflowId())
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "No appointment is owned by payment workflow " + context.workflowId()));
        confirmations.confirm(appointmentId);
        yield new WorkflowHandle(context.workflowId(), WorkflowStatus.SUCCEEDED, 1);
      }
      case "DECLINED", "CANCELLED", "REFUNDED" ->
          new WorkflowHandle(context.workflowId(), WorkflowStatus.FAILED, 1);
      default -> throw new IllegalStateException("Unsupported payment workflow status");
    };
  }
}
