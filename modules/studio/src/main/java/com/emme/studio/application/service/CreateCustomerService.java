package com.emme.studio.application.service;

import com.emme.studio.api.usecase.CreateCustomerUseCase;
import com.emme.studio.application.port.out.CustomerRepository;
import com.emme.studio.domain.model.Customer;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for customer creation. */
@Service
@Transactional
public class CreateCustomerService implements CreateCustomerUseCase {

  private final CustomerRepository customerRepository;

  public CreateCustomerService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  @Override
  public Customer create(UUID tenantId, String name, String phone, String email) {
    Customer customer = new Customer(tenantId, name);
    customer.setPhone(phone);
    customer.setEmail(email);
    return customerRepository.save(customer);
  }
}
