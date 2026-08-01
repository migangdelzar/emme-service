package com.emme.payment.application.service;

import com.emme.payment.api.command.ProcessPaymentCallbackCommand;
import com.emme.payment.api.result.PaymentInfo;
import com.emme.payment.api.usecase.ProcessPaymentCallbackUseCase;
import com.emme.payment.application.mapper.PaymentApplicationMapper;
import com.emme.payment.application.port.out.PaymentProvider;
import com.emme.payment.application.port.out.PaymentProviderException;
import com.emme.payment.application.port.out.PaymentRepository;
import com.emme.payment.domain.model.Payment;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProcessPaymentCallbackService implements ProcessPaymentCallbackUseCase {
  private final PaymentRepository repository;
  private final PaymentProvider provider;

  public ProcessPaymentCallbackService(PaymentRepository repository, PaymentProvider provider) {
    this.repository = repository;
    this.provider = provider;
  }

  @Override
  public PaymentInfo process(ProcessPaymentCallbackCommand command) {
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
}
