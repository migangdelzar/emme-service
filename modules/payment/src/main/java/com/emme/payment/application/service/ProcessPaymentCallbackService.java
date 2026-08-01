package com.emme.payment.application.service;

import com.emme.payment.api.command.ProcessPaymentCallbackCommand;
import com.emme.payment.api.result.PaymentInfo;
import com.emme.payment.api.usecase.ProcessPaymentCallbackUseCase;
import com.emme.payment.application.mapper.PaymentApplicationMapper;
import com.emme.payment.application.port.out.PaymentProvider;
import com.emme.payment.application.port.out.PaymentProviderException;
import com.emme.payment.application.port.out.PaymentRepository;
import com.emme.payment.application.port.out.PaymentWebhookEventRepository;
import com.emme.payment.domain.model.Payment;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProcessPaymentCallbackService implements ProcessPaymentCallbackUseCase {
  private final PaymentRepository repository;
  private final PaymentProvider provider;
  private final PaymentWebhookEventRepository webhookEvents;

  public ProcessPaymentCallbackService(
      PaymentRepository repository,
      PaymentProvider provider,
      PaymentWebhookEventRepository webhookEvents) {
    this.repository = repository;
    this.provider = provider;
    this.webhookEvents = webhookEvents;
  }

  @Override
  public PaymentInfo process(ProcessPaymentCallbackCommand command) {
    if (command.eventId() == null || command.eventId().isBlank()) {
      throw new PaymentProviderException("Payment callback event id is required");
    }
    if (!webhookEvents.claim(command.tenantId(), command.provider(), command.eventId())) {
      String providerReference = providerReference(command.payload());
      return repository
          .findByTenantIdAndProviderReference(command.tenantId(), providerReference)
          .map(PaymentApplicationMapper::toInfo)
          .orElseThrow(
              () -> new PaymentProviderException("Duplicate callback has no payment record"));
    }
    PaymentProvider.PaymentResult result =
        provider.handleCallback(command.payload(), command.signature());
    if (result == null || result.providerTransactionId() == null) {
      throw new PaymentProviderException("Provider handleCallback returned null transaction ID");
    }
    Payment payment =
        repository
            .findByTenantIdAndProviderReference(command.tenantId(), result.providerTransactionId())
            .orElseGet(
                () ->
                    new Payment(
                        command.tenantId(),
                        result.providerTransactionId(),
                        BigDecimal.ZERO,
                        "MXN"));
    payment.applyProviderStatus(PaymentServiceSupport.status(result.status()));
    return PaymentApplicationMapper.toInfo(repository.save(payment));
  }

  private String providerReference(java.util.Map<String, String> payload) {
    String direct = payload.get("id");
    return direct != null ? direct : payload.getOrDefault("data.id", "");
  }
}
