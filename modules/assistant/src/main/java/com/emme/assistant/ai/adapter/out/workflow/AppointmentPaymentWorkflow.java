package com.emme.assistant.ai.adapter.out.workflow;

import com.emme.ai.contracts.payment.PaymentWorkflowEvent;
import com.emme.ai.contracts.workflow.PaymentWorkflow;
import com.emme.ai.contracts.workflow.WorkflowHandle;
import com.emme.ai.contracts.workflow.WorkflowStatus;
import com.emme.kernel.context.AiExecutionContext;
import java.util.Objects;

/** Projects verified payment events into durable booking workflow lifecycle states. */
public final class AppointmentPaymentWorkflow implements PaymentWorkflow {

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
      case "CAPTURED" -> new WorkflowHandle(context.workflowId(), WorkflowStatus.SUCCEEDED, 1);
      case "DECLINED", "CANCELLED", "REFUNDED" ->
          new WorkflowHandle(context.workflowId(), WorkflowStatus.FAILED, 1);
      default -> throw new IllegalStateException("Unsupported payment workflow status");
    };
  }
}
