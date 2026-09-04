package com.emme.clients.application.service;

import com.emme.clients.api.result.CustomerDetails;
import com.emme.clients.api.usecase.GetCustomerUseCase;
import com.emme.clients.application.mapper.CustomerApplicationMapper;
import com.emme.clients.application.port.out.CustomerRepository;
import com.emme.kernel.context.TenantContextHolder;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for customer retrieval. */
@Service
@Transactional(readOnly = true)
public class GetCustomerService implements GetCustomerUseCase {

  private final CustomerRepository customerRepository;

  public GetCustomerService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  @Override
  public Optional<CustomerDetails> get(UUID id) {
    return customerRepository
        .findByTenantIdAndId(TenantContextHolder.requireCurrentTenantId(), id)
        .map(CustomerApplicationMapper::toDetails);
  }
}
