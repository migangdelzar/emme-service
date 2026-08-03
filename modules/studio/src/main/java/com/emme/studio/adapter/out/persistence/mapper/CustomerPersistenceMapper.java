package com.emme.studio.adapter.out.persistence.mapper;

import com.emme.studio.adapter.out.persistence.entity.CustomerEntity;
import com.emme.studio.domain.model.Customer;

/** Translates the Customer domain model to and from its JPA representation. */
public final class CustomerPersistenceMapper {

  public Customer toDomain(CustomerEntity entity) {
    return Customer.reconstitute(
        entity.getId(),
        entity.getTenantId(),
        entity.getName(),
        entity.getPhone(),
        entity.getEmail(),
        entity.getStatus());
  }

  public void updateEntity(Customer domain, CustomerEntity entity) {
    entity.setName(domain.getName());
    entity.setPhone(domain.getPhone());
    entity.setEmail(domain.getEmail());
    entity.setStatus(domain.getStatus());
  }

  public CustomerEntity toNewEntity(Customer domain) {
    CustomerEntity entity = new CustomerEntity(domain.getTenantId(), domain.getName());
    updateEntity(domain, entity);
    return entity;
  }
}
