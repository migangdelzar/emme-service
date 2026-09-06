package com.emme.assistant.ai.adapter.in.messaging;

import com.emme.ai.contracts.payment.PaymentWorkflowEvent;
import com.emme.ai.contracts.workflow.PaymentWorkflow;
import com.emme.assistant.ai.application.port.out.PaymentWorkflowExecutionContextRepository;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.kernel.context.TenantContextHolder;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Restores trusted tenant and workflow identity before applying a payment callback. */
@Component
@ConditionalOnBean(PaymentWorkflow.class)
public final class PaymentWorkflowEventListener {

  private final PaymentWorkflow workflow;
  private final PaymentWorkflowExecutionContextRepository contexts;

  public PaymentWorkflowEventListener(
      PaymentWorkflow workflow, PaymentWorkflowExecutionContextRepository contexts) {
    this.workflow = Objects.requireNonNull(workflow, "workflow must not be null");
    this.contexts = Objects.requireNonNull(contexts, "contexts must not be null");
  }

  @ApplicationModuleListener(id = "assistant.payment-workflow-event")
  public void onPaymentWorkflow(PaymentWorkflowEvent event) {
    Objects.requireNonNull(event, "event must not be null");
    TenantContextHolder.withTenantAndCorrelation(
        event.tenantId(),
        "payment-workflow:" + event.eventId(),
        () -> {
          PaymentWorkflowExecutionContextRepository.WorkflowExecutionContext persisted =
              contexts
                  .findByWorkflowId(event.workflowId())
                  .orElseThrow(
                      () ->
                          new SecurityException(
                              "Payment callback has no persisted workflow context"));
          AiExecutionContext context =
              new AiExecutionContext(
                  event.tenantId(),
                  persisted.principalId(),
                  java.util.Set.of(),
                  persisted.conversationId(),
                  event.workflowId(),
                  "payment-workflow:" + event.eventId(),
                  persisted.idempotencyKey());
          AiExecutionContextScope.run(
              context,
              () -> {
                workflow.resume(event, context);
              });
        });
  }
}
