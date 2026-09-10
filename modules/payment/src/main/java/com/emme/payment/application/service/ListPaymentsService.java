package com.emme.payment.application.service;

import com.emme.payment.api.query.ListPaymentsQuery;
import com.emme.payment.api.result.PaymentDetails;
import com.emme.payment.api.usecase.ListPaymentsUseCase;
import com.emme.payment.application.mapper.PaymentApplicationMapper;
import com.emme.payment.application.port.out.PaymentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ListPaymentsService implements ListPaymentsUseCase {
  private final PaymentRepository repository;

  @Override
  public List<PaymentDetails> list(ListPaymentsQuery query) {
    return repository.findAll().stream().map(PaymentApplicationMapper::toResult).toList();
  }
}
