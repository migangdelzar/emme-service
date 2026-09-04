package com.emme.clients.adapter.out.persistence.adapter;

import com.emme.clients.adapter.out.persistence.entity.CustomerEntity;
import com.emme.clients.adapter.out.persistence.mapper.CustomerPersistenceMapper;
import com.emme.clients.adapter.out.persistence.repository.SpringDataCustomerRepository;
import com.emme.clients.application.port.out.CustomerRepository;
import com.emme.clients.domain.model.Customer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements the Customer persistence port using Spring Data JPA. */
@Component
public class CustomerPersistenceAdapter implements CustomerRepository {

  private final SpringDataCustomerRepository repository;
  private final CustomerPersistenceMapper mapper;

  public CustomerPersistenceAdapter(SpringDataCustomerRepository repository) {
    this.repository = repository;
    this.mapper = new CustomerPersistenceMapper();
  }

  @Override
  public Customer save(Customer customer) {
    CustomerEntity entity =
        customer.getId() == null
            ? mapper.toNewEntity(customer)
            : repository
                .findByTenantIdAndId(customer.getTenantId(), customer.getId())
                .orElseThrow();
    mapper.updateEntity(customer, entity);
    return mapper.toDomain(repository.save(entity));
  }

  @Override
  public Optional<Customer> findByTenantIdAndId(UUID tenantId, UUID id) {
    return repository.findByTenantIdAndId(tenantId, id).map(mapper::toDomain);
  }

  @Override
  public List<Customer> findByTenantId(UUID tenantId) {
    return repository.findByTenantId(tenantId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Customer> searchByName(UUID tenantId, String name) {
    return repository.findByTenantIdAndNameContainingIgnoreCase(tenantId, name).stream()
        .map(mapper::toDomain)
        .toList();
  }
}
