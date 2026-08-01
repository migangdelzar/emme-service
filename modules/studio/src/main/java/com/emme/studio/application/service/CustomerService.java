package com.emme.studio.application.service;

import com.emme.studio.adapter.out.persistence.entity.CustomerEntity;
import com.emme.studio.adapter.out.persistence.repository.SpringDataCustomerRepository;
import com.emme.studio.domain.model.CustomerStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerService {

  private final SpringDataCustomerRepository customerRepository;

  public CustomerService(SpringDataCustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  public CustomerEntity create(UUID tenantId, String name, String phone, String email) {
    CustomerEntity customer = new CustomerEntity(tenantId, name);
    customer.setPhone(phone);
    customer.setEmail(email);
    return customerRepository.save(customer);
  }

  public CustomerEntity update(UUID id, String name, String phone, String email) {
    CustomerEntity customer =
        customerRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("CustomerEntity not found: " + id));
    customer.setName(name);
    customer.setPhone(phone);
    customer.setEmail(email);
    return customerRepository.save(customer);
  }

  public CustomerEntity retire(UUID id) {
    CustomerEntity customer =
        customerRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("CustomerEntity not found: " + id));
    customer.setStatus(CustomerStatus.RETIRED);
    return customerRepository.save(customer);
  }

  @Transactional(readOnly = true)
  public Optional<CustomerEntity> findById(UUID id) {
    return customerRepository.findById(id);
  }

  @Transactional(readOnly = true)
  public List<CustomerEntity> findByTenantId(UUID tenantId) {
    return customerRepository.findByTenantId(tenantId);
  }

  @Transactional(readOnly = true)
  public List<CustomerEntity> searchByName(UUID tenantId, String query) {
    return customerRepository.findByTenantIdAndNameContainingIgnoreCase(tenantId, query);
  }
}
