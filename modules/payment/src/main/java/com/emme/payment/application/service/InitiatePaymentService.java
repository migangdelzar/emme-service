package com.emme.payment.application.service;

import com.emme.payment.api.command.InitiatePaymentCommand;
import com.emme.payment.api.result.PaymentInfo;
import com.emme.payment.api.usecase.InitiatePaymentUseCase;
import com.emme.payment.application.mapper.PaymentApplicationMapper;
import com.emme.payment.application.port.out.PaymentProvider;
import com.emme.payment.application.port.out.PaymentProviderException;
import com.emme.payment.application.port.out.PaymentRepository;
import com.emme.payment.domain.model.Payment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InitiatePaymentService implements InitiatePaymentUseCase {
  private final PaymentRepository repository;
  private final PaymentProvider provider;

  public InitiatePaymentService(PaymentRepository repository, PaymentProvider provider) {
    this.repository = repository;
    this.provider = provider;
  }

  @Override
  public PaymentInfo initiate(InitiatePaymentCommand command) {
    return repository
        .findByTenantIdAndProviderReference(command.tenantId(), command.providerReference())
        .map(PaymentApplicationMapper::toInfo)
        .orElseGet(() -> create(command));
  }

  private PaymentInfo create(InitiatePaymentCommand command) {
    PaymentProvider.PaymentResult result =
        provider.initiate(
            command.tenantId() + "/" + java.util.UUID.randomUUID(),
            command.amount(),
            command.currency(),
            "Payment for tenant " + command.tenantId());
    if (result == null || result.providerTransactionId() == null) {
      throw new PaymentProviderException("Provider initiate returned null transaction ID");
    }
    Payment payment =
        new Payment(
            command.tenantId(),
            result.providerTransactionId(),
            command.amount(),
            command.currency());
    return PaymentApplicationMapper.toInfo(repository.save(payment));
  }
}
