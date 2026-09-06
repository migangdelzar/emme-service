package com.emme.assistant.ai.adapter.out.workflow;

import com.emme.ai.contracts.workflow.ConversationWorkflow;
import com.emme.ai.contracts.workflow.WorkflowCommand;
import com.emme.ai.contracts.workflow.WorkflowHandle;
import com.emme.ai.contracts.workflow.WorkflowStatus;
import com.emme.appointments.api.command.CreateAppointmentHoldCommand;
import com.emme.appointments.api.usecase.CreateAppointmentHoldUseCase;
import com.emme.assistant.ai.application.port.out.PaymentWorkflowCheckpointRepository;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.payment.api.command.CreatePaymentLinkCommand;
import com.emme.payment.api.usecase.CreatePaymentLinkUseCase;
import java.util.Objects;
import java.util.UUID;

/** Coordinates the confirmation, hold, and optional payment-link application boundaries. */
public final class AppointmentBookingWorkflow implements ConversationWorkflow {

  private static final String WORKFLOW_TYPE = "appointment_booking";

  private final CreateAppointmentHoldUseCase holds;
  private final CreatePaymentLinkUseCase paymentLinks;
  private final PaymentWorkflowCheckpointRepository checkpoints;

  public AppointmentBookingWorkflow(
      CreateAppointmentHoldUseCase holds,
      CreatePaymentLinkUseCase paymentLinks,
      PaymentWorkflowCheckpointRepository checkpoints) {
    this.holds = Objects.requireNonNull(holds, "holds must not be null");
    this.paymentLinks = Objects.requireNonNull(paymentLinks, "paymentLinks must not be null");
    this.checkpoints = Objects.requireNonNull(checkpoints, "checkpoints must not be null");
  }

  @Override
  public WorkflowHandle startOrResume(WorkflowCommand command, AiExecutionContext context) {
    Objects.requireNonNull(command, "command must not be null");
    Objects.requireNonNull(context, "context must not be null");
    validateCorrelation(command, context);
    if (!Boolean.TRUE.equals(command.input().get("confirmed"))) {
      return record(
          context,
          new WorkflowHandle(context.workflowId(), WorkflowStatus.WAITING_FOR_CONFIRMATION, 0));
    }

    UUID appointmentId = appointmentId(command);
    var hold =
        holds.create(new CreateAppointmentHoldCommand(appointmentId, command.idempotencyKey()));
    if (!Boolean.TRUE.equals(command.input().get("requiresPayment"))) {
      return record(context, new WorkflowHandle(context.workflowId(), WorkflowStatus.SUCCEEDED, 1));
    }

    paymentLinks.create(
        new CreatePaymentLinkCommand(
            context.workflowId(), hold.holdId(), "workflow-payment-" + context.workflowId()));
    return record(
        context, new WorkflowHandle(context.workflowId(), WorkflowStatus.WAITING_FOR_PAYMENT, 1));
  }

  private WorkflowHandle record(AiExecutionContext context, WorkflowHandle handle) {
    checkpoints.record(context, handle);
    return handle;
  }

  private static void validateCorrelation(WorkflowCommand command, AiExecutionContext context) {
    if (!WORKFLOW_TYPE.equals(command.workflowType())) {
      throw new IllegalArgumentException("Unsupported appointment workflow type");
    }
    if (!context.workflowId().equals(command.workflowId())) {
      throw new SecurityException("workflowId does not match AI execution context");
    }
    if (!context.idempotencyKey().equals(command.idempotencyKey())) {
      throw new SecurityException("idempotencyKey does not match AI execution context");
    }
  }

  private static UUID appointmentId(WorkflowCommand command) {
    Object value = command.input().get("appointmentId");
    if (!(value instanceof String text) || text.isBlank()) {
      throw new IllegalArgumentException("appointmentId is required");
    }
    try {
      return UUID.fromString(text);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("appointmentId must be a UUID", exception);
    }
  }
}
