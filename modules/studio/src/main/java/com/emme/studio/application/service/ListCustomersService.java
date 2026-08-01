package com.emme.studio.application.service;

import com.emme.studio.api.result.CustomerInfo;
import com.emme.studio.api.usecase.ListCustomersUseCase;
import com.emme.studio.application.port.out.CustomerRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for the public customer-list query. */
@Service
@Transactional(readOnly = true)
public class ListCustomersService implements ListCustomersUseCase {

  private final CustomerRepository customerRepository;

  public ListCustomersService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  @Override
  public List<CustomerInfo> listCustomers(UUID tenantId) {
    return customerRepository.findByTenantId(tenantId).stream()
        .map(c -> new CustomerInfo(c.getId(), c.getName(), c.getPhone(), c.getEmail()))
        .toList();
  }
}
