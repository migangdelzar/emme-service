package com.emme.payment.application.service;

import com.emme.payment.api.command.CapturePaymentCommand;
import com.emme.payment.api.result.PaymentDetails;
import com.emme.payment.api.usecase.CapturePaymentUseCase;
import com.emme.payment.application.mapper.PaymentApplicationMapper;
import com.emme.payment.application.port.out.PaymentProvider;
import com.emme.payment.application.port.out.PaymentRepository;
import com.emme.payment.domain.model.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CapturePaymentService implements CapturePaymentUseCase {
  private final PaymentRepository repository;
  private final PaymentProvider provider;

  @Override
  public PaymentDetails capture(CapturePaymentCommand command) {
    Payment payment = PaymentServiceSupport.load(repository, command.paymentId());
    PaymentProvider.PaymentResult result =
        provider.capture(payment.providerReference(), payment.amount());
    payment.applyProviderStatus(PaymentServiceSupport.status(result.status()));
    return PaymentApplicationMapper.toResult(repository.save(payment));
  }
}
