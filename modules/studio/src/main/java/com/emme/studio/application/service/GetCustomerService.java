package com.emme.studio.application.service;

import com.emme.studio.api.result.CustomerDetails;
import com.emme.studio.api.usecase.GetCustomerUseCase;
import com.emme.studio.application.mapper.CustomerApplicationMapper;
import com.emme.studio.application.port.out.CustomerRepository;
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
    return customerRepository.findById(id).map(CustomerApplicationMapper::toDetails);
  }
}
