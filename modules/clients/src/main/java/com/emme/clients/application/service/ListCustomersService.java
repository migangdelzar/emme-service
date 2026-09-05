package com.emme.clients.application.service;

import com.emme.clients.api.result.CustomerSummary;
import com.emme.clients.api.usecase.ListCustomersUseCase;
import com.emme.clients.application.port.out.CustomerRepository;
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
  public List<CustomerSummary> listCustomers(UUID tenantId) {
    return customerRepository.findAll().stream()
        .map(c -> new CustomerSummary(c.getId(), c.getName(), c.getPhone(), c.getEmail()))
        .toList();
  }
}
