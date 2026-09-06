package com.emme.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.payment.PaymentWorkflowEvent;
import com.emme.payment.api.command.ProcessPaymentCallbackCommand;
import com.emme.payment.api.result.PaymentDetails;
import com.emme.payment.api.type.PaymentStatus;
import com.emme.payment.api.usecase.ProcessPaymentCallbackUseCase;
import com.emme.payment.application.port.out.PaymentProviderException;
import com.emme.payment.application.port.out.PaymentWorkflowCorrelationRepository;
import com.emme.payment.application.port.out.PaymentWorkflowEventPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessPaymentWorkflowCallbackServiceTest {

  @Test
  void emitsTheCanonicalEventAfterTheIdempotentPaymentCallbackIsProcessed() {
    UUID tenantId = UUID.randomUUID();
    UUID workflowId = UUID.randomUUID();
    ProcessPaymentCallbackCommand command =
        new ProcessPaymentCallbackCommand(
            tenantId, "mock", "event-1", Map.of("id", "provider-payment-1"), "signature");
    ProcessPaymentCallbackUseCase callback = mock(ProcessPaymentCallbackUseCase.class);
    PaymentWorkflowCorrelationRepository correlations =
        mock(PaymentWorkflowCorrelationRepository.class);
    PaymentWorkflowEventPublisher events = mock(PaymentWorkflowEventPublisher.class);
    when(callback.process(command))
        .thenReturn(
            new PaymentDetails(
                UUID.randomUUID(),
                tenantId,
                "provider-payment-1",
                BigDecimal.TEN,
                "MXN",
                PaymentStatus.CAPTURED,
                Instant.parse("2030-01-01T09:00:00Z")));
    when(correlations.findByProviderAndProviderReference("mock", "provider-payment-1"))
        .thenReturn(
            java.util.Optional.of(
                new PaymentWorkflowCorrelationRepository.PaymentWorkflowCorrelation(
                    workflowId, "mock", "provider-payment-1")));

    PaymentWorkflowEvent event =
        new ProcessPaymentWorkflowCallbackService(callback, correlations, events).process(command);

    assertThat(event.workflowId()).isEqualTo(workflowId);
    assertThat(event.provider()).isEqualTo("mock");
    assertThat(event.eventId()).isEqualTo("event-1");
    assertThat(event.providerReference()).isEqualTo("provider-payment-1");
    assertThat(event.status()).isEqualTo("CAPTURED");
    verify(callback).process(command);
    verify(events).publish(event);
  }

  @Test
  void rejectsAProcessedCallbackWithoutTrustedWorkflowOwnership() {
    UUID tenantId = UUID.randomUUID();
    ProcessPaymentCallbackCommand command =
        new ProcessPaymentCallbackCommand(
            tenantId, "mock", "event-1", Map.of("id", "provider-payment-1"), "signature");
    ProcessPaymentCallbackUseCase callback = mock(ProcessPaymentCallbackUseCase.class);
    PaymentWorkflowCorrelationRepository correlations =
        mock(PaymentWorkflowCorrelationRepository.class);
    when(callback.process(command))
        .thenReturn(
            new PaymentDetails(
                UUID.randomUUID(),
                tenantId,
                "provider-payment-1",
                BigDecimal.TEN,
                "MXN",
                PaymentStatus.CAPTURED,
                Instant.parse("2030-01-01T09:00:00Z")));
    when(correlations.findByProviderAndProviderReference("mock", "provider-payment-1"))
        .thenReturn(java.util.Optional.empty());

    assertThatThrownBy(
            () ->
                new ProcessPaymentWorkflowCallbackService(
                        callback, correlations, mock(PaymentWorkflowEventPublisher.class))
                    .process(command))
        .isInstanceOf(PaymentProviderException.class)
        .hasMessageContaining("no workflow correlation");
  }
}
