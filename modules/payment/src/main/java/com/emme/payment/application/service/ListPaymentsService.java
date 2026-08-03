package com.emme.payment.application.service;

import com.emme.payment.api.query.ListPaymentsQuery;
import com.emme.payment.api.result.PaymentInfo;
import com.emme.payment.api.usecase.ListPaymentsUseCase;
import com.emme.payment.application.mapper.PaymentApplicationMapper;
import com.emme.payment.application.port.out.PaymentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListPaymentsService implements ListPaymentsUseCase {
  private final PaymentRepository repository;

  public ListPaymentsService(PaymentRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<PaymentInfo> list(ListPaymentsQuery query) {
    return repository.findByTenantId(query.tenantId()).stream()
        .map(PaymentApplicationMapper::toInfo)
        .toList();
  }
}
