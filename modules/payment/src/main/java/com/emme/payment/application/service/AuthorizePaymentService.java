package com.emme.payment.application.service;

import com.emme.payment.api.command.AuthorizePaymentCommand;
import com.emme.payment.api.result.PaymentDetails;
import com.emme.payment.api.usecase.AuthorizePaymentUseCase;
import com.emme.payment.application.mapper.PaymentApplicationMapper;
import com.emme.payment.application.port.out.PaymentProvider;
import com.emme.payment.application.port.out.PaymentRepository;
import com.emme.payment.domain.model.Payment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthorizePaymentService implements AuthorizePaymentUseCase {
  private final PaymentRepository repository;
  private final PaymentProvider provider;

  public AuthorizePaymentService(PaymentRepository repository, PaymentProvider provider) {
    this.repository = repository;
    this.provider = provider;
  }

  @Override
  public PaymentDetails authorize(AuthorizePaymentCommand command) {
    Payment payment =
        PaymentServiceSupport.load(repository, command.tenantId(), command.paymentId());
    PaymentProvider.PaymentResult result = provider.authorize(payment.providerReference());
    payment.applyProviderStatus(PaymentServiceSupport.status(result.status()));
    return PaymentApplicationMapper.toResult(repository.save(payment));
  }
}
