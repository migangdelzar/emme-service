package com.emme.studio.application.service;

import com.emme.studio.api.usecase.UpdateCustomerUseCase;
import com.emme.studio.application.port.out.CustomerRepository;
import com.emme.studio.domain.model.Customer;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for customer updates. */
@Service
@Transactional
public class UpdateCustomerService implements UpdateCustomerUseCase {

  private final CustomerRepository customerRepository;

  public UpdateCustomerService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  @Override
  public Customer update(UUID id, String name, String phone, String email) {
    Customer customer =
        customerRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
    customer.setName(name);
    customer.setPhone(phone);
    customer.setEmail(email);
    return customerRepository.save(customer);
  }
}
