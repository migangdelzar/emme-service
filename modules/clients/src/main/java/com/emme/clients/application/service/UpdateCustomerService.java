package com.emme.clients.application.service;

import com.emme.clients.api.result.CustomerDetails;
import com.emme.clients.api.usecase.UpdateCustomerUseCase;
import com.emme.clients.application.mapper.CustomerApplicationMapper;
import com.emme.clients.application.port.out.CustomerRepository;
import com.emme.clients.domain.model.Customer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for customer updates. */
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateCustomerService implements UpdateCustomerUseCase {

  private final CustomerRepository customerRepository;

  @Override
  public CustomerDetails update(UUID id, String name, String phone, String email) {
    Customer customer =
        customerRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
    customer.setName(name);
    customer.setPhone(phone);
    customer.setEmail(email);
    return CustomerApplicationMapper.toDetails(customerRepository.save(customer));
  }
}
