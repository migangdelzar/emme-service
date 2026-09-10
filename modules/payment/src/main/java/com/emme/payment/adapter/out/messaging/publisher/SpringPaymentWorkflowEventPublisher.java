package com.emme.payment.adapter.out.messaging.publisher;

import com.emme.ai.contracts.payment.PaymentWorkflowEvent;
import com.emme.payment.application.port.out.PaymentWorkflowEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Publishes verified payment workflow facts through Spring Modulith. */
@Component
@RequiredArgsConstructor
public final class SpringPaymentWorkflowEventPublisher implements PaymentWorkflowEventPublisher {

  private final ApplicationEventPublisher publisher;

  @Override
  public void publish(PaymentWorkflowEvent event) {
    publisher.publishEvent(event);
  }
}
