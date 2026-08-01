package com.emme.studio.application.service;

import com.emme.studio.api.usecase.GetCustomerUseCase;
import com.emme.studio.application.port.out.CustomerRepository;
import com.emme.studio.domain.model.Customer;
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
  public Optional<Customer> get(UUID id) {
    return customerRepository.findById(id);
  }
}
