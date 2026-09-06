package com.emme.payment.application.service;

import com.emme.ai.contracts.payment.PaymentWorkflowEvent;
import com.emme.ai.contracts.payment.PaymentWorkflowStatus;
import com.emme.payment.api.command.ProcessPaymentCallbackCommand;
import com.emme.payment.api.port.out.PaymentWorkflowCorrelationRepository;
import com.emme.payment.api.port.out.PaymentWorkflowCorrelationRepository.PaymentWorkflowCorrelation;
import com.emme.payment.api.result.PaymentDetails;
import com.emme.payment.api.usecase.ProcessPaymentCallbackUseCase;
import com.emme.payment.api.usecase.ProcessPaymentWorkflowCallbackUseCase;
import com.emme.payment.application.port.out.PaymentProviderException;
import com.emme.payment.application.port.out.PaymentWorkflowEventPublisher;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Converts the payment callback result into the provider-neutral workflow event contract. */
@Service
@Transactional
public class ProcessPaymentWorkflowCallbackService
    implements ProcessPaymentWorkflowCallbackUseCase {

  private final ProcessPaymentCallbackUseCase callback;
  private final PaymentWorkflowCorrelationRepository correlations;
  private final PaymentWorkflowEventPublisher events;

  public ProcessPaymentWorkflowCallbackService(
      ProcessPaymentCallbackUseCase callback,
      PaymentWorkflowCorrelationRepository correlations,
      PaymentWorkflowEventPublisher events) {
    this.callback = Objects.requireNonNull(callback, "callback must not be null");
    this.correlations = Objects.requireNonNull(correlations, "correlations must not be null");
    this.events = Objects.requireNonNull(events, "events must not be null");
  }

  @Override
  public PaymentWorkflowEvent process(ProcessPaymentCallbackCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    PaymentDetails payment = callback.process(command);
    PaymentWorkflowCorrelation correlation =
        correlations
            .findByProviderAndProviderReference(command.provider(), payment.providerReference())
            .orElseThrow(
                () ->
                    new PaymentProviderException(
                        "Payment callback has no workflow correlation for provider reference "
                            + payment.providerReference()));
    PaymentWorkflowEvent event =
        new PaymentWorkflowEvent(
            command.tenantId(),
            correlation.workflowId(),
            correlation.provider(),
            command.eventId(),
            correlation.providerReference(),
            PaymentWorkflowStatus.valueOf(payment.status().name()));
    events.publish(event);
    return event;
  }
}
