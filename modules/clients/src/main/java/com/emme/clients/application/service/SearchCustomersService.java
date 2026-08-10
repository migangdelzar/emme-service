package com.emme.clients.application.service;

import com.emme.clients.api.result.CustomerDetails;
import com.emme.clients.api.usecase.SearchCustomersUseCase;
import com.emme.clients.application.mapper.CustomerApplicationMapper;
import com.emme.clients.application.port.out.CustomerRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for customer name search. */
@Service
@Transactional(readOnly = true)
public class SearchCustomersService implements SearchCustomersUseCase {

  private final CustomerRepository customerRepository;

  public SearchCustomersService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  @Override
  public List<CustomerDetails> search(UUID tenantId, String query) {
    return customerRepository.searchByName(tenantId, query).stream()
        .map(CustomerApplicationMapper::toDetails)
        .toList();
  }
}
