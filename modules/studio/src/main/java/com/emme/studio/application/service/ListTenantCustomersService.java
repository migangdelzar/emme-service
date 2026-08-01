package com.emme.studio.application.service;

import com.emme.studio.api.usecase.ListTenantCustomersUseCase;
import com.emme.studio.application.port.out.CustomerRepository;
import com.emme.studio.domain.model.Customer;
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
  public List<Customer> list(UUID tenantId) {
    return customerRepository.findByTenantId(tenantId);
  }
}
