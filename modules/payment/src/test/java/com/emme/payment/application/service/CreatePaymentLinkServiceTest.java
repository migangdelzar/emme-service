package com.emme.payment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.payment.PaymentLink;
import com.emme.payment.api.command.CreatePaymentLinkCommand;
import com.emme.payment.application.port.out.PaymentLinkRepository;
import com.emme.payment.application.port.out.PaymentLinkSourceRepository;
import com.emme.payment.application.port.out.PaymentProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreatePaymentLinkServiceTest {

  @Test
  void createsALinkFromTrustedPersistedPaymentState() {
    UUID workflowId = UUID.randomUUID();
    UUID holdId = UUID.randomUUID();
    Instant expiresAt = Instant.parse("2030-01-01T09:15:00Z");
    PaymentLinkRepository links = mock(PaymentLinkRepository.class);
    PaymentLinkSourceRepository sources = mock(PaymentLinkSourceRepository.class);
    PaymentProvider provider = mock(PaymentProvider.class);
    when(links.findByIdempotencyKey("payment-1")).thenReturn(Optional.empty());
    when(sources.findByWorkflowIdAndHoldId(workflowId, holdId))
        .thenReturn(
            Optional.of(
                new PaymentLinkSourceRepository.PaymentLinkSource(
                    new BigDecimal("125.00"), "MXN", "Appointment", expiresAt)));
    when(provider.name()).thenReturn("mock");
    when(provider.initiate("payment-1", new BigDecimal("125.00"), "MXN", "Appointment"))
        .thenReturn(
            new PaymentProvider.PaymentResult(
                "provider-payment-1", "PENDING", Map.of("checkout_url", "https://pay.test/1")));
    when(links.save(any(), eq("payment-1"))).thenAnswer(invocation -> invocation.getArgument(0));

    PaymentLink link =
        new CreatePaymentLinkService(links, sources, provider)
            .create(new CreatePaymentLinkCommand(workflowId, holdId, "payment-1"));

    assertThat(link.workflowId()).isEqualTo(workflowId);
    assertThat(link.provider()).isEqualTo("mock");
    assertThat(link.checkoutUrl()).isEqualTo("https://pay.test/1");
    assertThat(link.expiresAt()).isEqualTo(expiresAt);
    verify(provider).initiate("payment-1", new BigDecimal("125.00"), "MXN", "Appointment");
  }

  @Test
  void reusesAnExistingLinkWithoutCallingTheProviderAgain() {
    UUID workflowId = UUID.randomUUID();
    UUID holdId = UUID.randomUUID();
    PaymentLink existing =
        new PaymentLink(
            UUID.randomUUID(),
            workflowId,
            "mock",
            "https://pay.test/1",
            Instant.parse("2030-01-01T09:15:00Z"));
    PaymentLinkRepository links = mock(PaymentLinkRepository.class);
    PaymentLinkSourceRepository sources = mock(PaymentLinkSourceRepository.class);
    PaymentProvider provider = mock(PaymentProvider.class);
    when(links.findByIdempotencyKey("payment-1")).thenReturn(Optional.of(existing));

    PaymentLink actual =
        new CreatePaymentLinkService(links, sources, provider)
            .create(new CreatePaymentLinkCommand(workflowId, holdId, "payment-1"));

    assertThat(actual).isEqualTo(existing);
    verifyNoInteractions(sources, provider);
  }
}
