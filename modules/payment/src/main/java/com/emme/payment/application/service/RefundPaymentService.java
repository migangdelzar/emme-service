package com.emme.payment.application.service;

import com.emme.payment.api.command.RefundPaymentCommand;
import com.emme.payment.api.result.PaymentDetails;
import com.emme.payment.api.usecase.RefundPaymentUseCase;
import com.emme.payment.application.mapper.PaymentApplicationMapper;
import com.emme.payment.application.port.out.PaymentProvider;
import com.emme.payment.application.port.out.PaymentRepository;
import com.emme.payment.domain.model.Payment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RefundPaymentService implements RefundPaymentUseCase {
  private final PaymentRepository repository;
  private final PaymentProvider provider;

  public RefundPaymentService(PaymentRepository repository, PaymentProvider provider) {
    this.repository = repository;
    this.provider = provider;
  }

  @Override
  public PaymentDetails refund(RefundPaymentCommand command) {
    Payment payment = PaymentServiceSupport.load(repository, command.paymentId());
    PaymentProvider.PaymentResult result =
        provider.refund(payment.providerReference(), payment.amount(), "Refund requested");
    payment.applyProviderStatus(PaymentServiceSupport.status(result.status()));
    return PaymentApplicationMapper.toResult(repository.save(payment));
  }
}
