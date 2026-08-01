package com.emme.payment.application.service;

import com.emme.payment.api.query.GetPaymentQuery;
import com.emme.payment.api.result.PaymentInfo;
import com.emme.payment.api.usecase.GetPaymentUseCase;
import com.emme.payment.application.mapper.PaymentApplicationMapper;
import com.emme.payment.application.port.out.PaymentRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetPaymentService implements GetPaymentUseCase {
  private final PaymentRepository repository;

  public GetPaymentService(PaymentRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<PaymentInfo> get(GetPaymentQuery query) {
    return repository.findById(query.paymentId()).map(PaymentApplicationMapper::toInfo);
  }
}
