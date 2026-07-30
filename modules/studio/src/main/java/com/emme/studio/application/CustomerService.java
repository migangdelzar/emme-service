package com.emme.studio.application;

import com.emme.studio.entity.Customer;
import com.emme.studio.entity.CustomerRepository;
import com.emme.studio.entity.CustomerStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerService {

  private final CustomerRepository customerRepository;

  public CustomerService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  public Customer create(UUID tenantId, String name, String phone, String email) {
    Customer customer = new Customer(tenantId, name);
    customer.setPhone(phone);
    customer.setEmail(email);
    return customerRepository.save(customer);
  }

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

  public Customer retire(UUID id) {
    Customer customer =
        customerRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
    customer.setStatus(CustomerStatus.RETIRED);
    return customerRepository.save(customer);
  }

  @Transactional(readOnly = true)
  public Optional<Customer> findById(UUID id) {
    return customerRepository.findById(id);
  }

  @Transactional(readOnly = true)
  public List<Customer> findByTenantId(UUID tenantId) {
    return customerRepository.findByTenantId(tenantId);
  }

  @Transactional(readOnly = true)
  public List<Customer> searchByName(UUID tenantId, String query) {
    return customerRepository.findByTenantIdAndNameContainingIgnoreCase(tenantId, query);
  }
}
