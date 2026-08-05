package com.emme.clients.application.service;

import com.emme.clients.api.result.CustomerDetails;
import com.emme.clients.api.usecase.CreateCustomerUseCase;
import com.emme.clients.application.mapper.CustomerApplicationMapper;
import com.emme.clients.application.port.out.CustomerRepository;
import com.emme.clients.domain.model.Customer;
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
  public CustomerDetails create(UUID tenantId, String name, String phone, String email) {
    Customer customer = new Customer(tenantId, name);
    customer.setPhone(phone);
    customer.setEmail(email);
    return CustomerApplicationMapper.toDetails(customerRepository.save(customer));
  }
}
