package com.emme.payment.application.service;

import com.emme.payment.api.query.GetPaymentQuery;
import com.emme.payment.api.result.PaymentDetails;
import com.emme.payment.api.usecase.GetPaymentUseCase;
import com.emme.payment.application.mapper.PaymentApplicationMapper;
import com.emme.payment.application.port.out.PaymentRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetPaymentService implements GetPaymentUseCase {
  private final PaymentRepository repository;

  @Override
  public Optional<PaymentDetails> get(GetPaymentQuery query) {
    return repository.findById(query.paymentId()).map(PaymentApplicationMapper::toResult);
  }
}
