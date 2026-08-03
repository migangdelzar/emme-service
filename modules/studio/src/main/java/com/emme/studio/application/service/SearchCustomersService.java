package com.emme.studio.application.service;

import com.emme.studio.api.usecase.SearchCustomersUseCase;
import com.emme.studio.application.port.out.CustomerRepository;
import com.emme.studio.domain.model.Customer;
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
  public List<Customer> search(UUID tenantId, String query) {
    return customerRepository.searchByName(tenantId, query);
  }
}
