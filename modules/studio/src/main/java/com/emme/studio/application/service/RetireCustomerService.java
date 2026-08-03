package com.emme.studio.application.service;

import com.emme.studio.api.result.CustomerDetails;
import com.emme.studio.api.usecase.RetireCustomerUseCase;
import com.emme.studio.application.mapper.CustomerApplicationMapper;
import com.emme.studio.application.port.out.CustomerRepository;
import com.emme.studio.domain.model.Customer;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for customer retirement. */
@Service
@Transactional
public class RetireCustomerService implements RetireCustomerUseCase {

  private final CustomerRepository customerRepository;

  public RetireCustomerService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  @Override
  public CustomerDetails retire(UUID id) {
    Customer customer =
        customerRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
    customer.retire();
    return CustomerApplicationMapper.toDetails(customerRepository.save(customer));
  }
}
