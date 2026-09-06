package com.emme.payment.application.port.out;

import com.emme.ai.contracts.payment.PaymentWorkflowEvent;

/** Publishes verified payment facts to the durable workflow boundary. */
public interface PaymentWorkflowEventPublisher {

  void publish(PaymentWorkflowEvent event);
}
