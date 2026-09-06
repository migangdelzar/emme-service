package com.emme.ai.contracts.workflow;

import com.emme.ai.contracts.payment.PaymentWorkflowEvent;
import com.emme.kernel.context.AiExecutionContext;

/** Payment callback boundary independent of provider SDKs and graph runtime types. */
@FunctionalInterface
public interface PaymentWorkflow {

  WorkflowHandle resume(PaymentWorkflowEvent event, AiExecutionContext context);
}
