package com.emme.assistant.ai.adapter.out.workflow;

import com.emme.ai.contracts.workflow.ConversationWorkflow;
import com.emme.ai.contracts.workflow.WorkflowCommand;
import com.emme.ai.contracts.workflow.WorkflowHandle;
import com.emme.ai.contracts.workflow.WorkflowStatus;
import com.emme.appointments.api.command.CancelAppointmentCommand;
import com.emme.appointments.api.type.AppointmentActor;
import com.emme.appointments.api.usecase.CancelAuthorizedAppointmentUseCase;
import com.emme.assistant.ai.application.port.out.WorkflowCheckpointRepository;
import com.emme.kernel.context.AiExecutionContext;
import java.util.Objects;
import java.util.UUID;

/** Coordinates confirmation and authorized appointment cancellation. */
public final class AppointmentCancellationWorkflow implements ConversationWorkflow {

  private static final String WORKFLOW_TYPE = "appointment_cancellation";

  private final CancelAuthorizedAppointmentUseCase cancellation;
  private final WorkflowCheckpointRepository checkpoints;

  public AppointmentCancellationWorkflow(
      CancelAuthorizedAppointmentUseCase cancellation, WorkflowCheckpointRepository checkpoints) {
    this.cancellation = Objects.requireNonNull(cancellation, "cancellation must not be null");
    this.checkpoints = Objects.requireNonNull(checkpoints, "checkpoints must not be null");
  }

  @Override
  public WorkflowHandle startOrResume(WorkflowCommand command, AiExecutionContext context) {
    Objects.requireNonNull(command, "command must not be null");
    Objects.requireNonNull(context, "context must not be null");
    validateCorrelation(command, context);
    if (!Boolean.TRUE.equals(command.input().get("confirmed"))) {
      WorkflowHandle handle =
          new WorkflowHandle(context.workflowId(), WorkflowStatus.WAITING_FOR_CONFIRMATION, 0);
      checkpoints.record(context, handle);
      return handle;
    }

    if (!checkpoints.claimForResume(context, WorkflowStatus.WAITING_FOR_CONFIRMATION)) {
      return new WorkflowHandle(context.workflowId(), WorkflowStatus.WAITING_FOR_CONFIRMATION, 0);
    }

    try {
      cancellation.cancel(
          new CancelAppointmentCommand(
              actor(context), requiredUuid(command, "appointmentId"), true));
    } catch (RuntimeException exception) {
      checkpoints.record(
          context,
          new WorkflowHandle(context.workflowId(), WorkflowStatus.WAITING_FOR_CONFIRMATION, 0));
      throw exception;
    }
    WorkflowHandle handle = new WorkflowHandle(context.workflowId(), WorkflowStatus.SUCCEEDED, 1);
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

  private static AppointmentActor actor(AiExecutionContext context) {
    return new AppointmentActor(
        context.tenantId(), context.principalId(), context.roles(), context.idempotencyKey());
  }

  private static UUID requiredUuid(WorkflowCommand command, String field) {
    Object value = command.input().get(field);
    if (!(value instanceof String text) || text.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
    try {
      return UUID.fromString(text);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(field + " must be a UUID", exception);
    }
  }
}
