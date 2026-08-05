package com.emme.clients.application.service;

import com.emme.clients.api.result.CustomerDetails;
import com.emme.clients.api.usecase.ListTenantCustomersUseCase;
import com.emme.clients.application.mapper.CustomerApplicationMapper;
import com.emme.clients.application.port.out.CustomerRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for listing customers in a tenant. */
@Service
@Transactional(readOnly = true)
public class ListTenantCustomersService implements ListTenantCustomersUseCase {

  private final CustomerRepository customerRepository;

  public ListTenantCustomersService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  @Override
  public List<CustomerDetails> list(UUID tenantId) {
    return customerRepository.findByTenantId(tenantId).stream()
        .map(CustomerApplicationMapper::toDetails)
        .toList();
  }
}
