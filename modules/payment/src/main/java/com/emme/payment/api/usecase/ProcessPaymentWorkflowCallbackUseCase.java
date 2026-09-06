package com.emme.payment.api.usecase;

import com.emme.ai.contracts.payment.PaymentWorkflowEvent;
import com.emme.payment.api.command.ProcessPaymentCallbackCommand;

/** Normalizes a verified, idempotently processed provider callback for workflow resumption. */
public interface ProcessPaymentWorkflowCallbackUseCase {

  PaymentWorkflowEvent process(ProcessPaymentCallbackCommand command);
}
