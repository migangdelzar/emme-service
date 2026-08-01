package com.emme.identity.adapter.out.persistence.adapter;

import com.emme.identity.adapter.out.persistence.mapper.CustomerMembershipPersistenceMapper;
import com.emme.identity.adapter.out.persistence.repository.SpringDataCustomerMembershipRepository;
import com.emme.identity.application.port.out.CustomerMembershipRepository;
import com.emme.identity.domain.model.CustomerMembership;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements customer membership persistence with Spring Data JPA. */
@Component
public final class CustomerMembershipPersistenceAdapter implements CustomerMembershipRepository {

  private final SpringDataCustomerMembershipRepository repository;
  private final CustomerMembershipPersistenceMapper mapper;

  public CustomerMembershipPersistenceAdapter(
      SpringDataCustomerMembershipRepository repository,
      CustomerMembershipPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public boolean existsByCustomerIdAndTenantId(UUID customerId, UUID tenantId) {
    return repository.existsByCustomerIdAndTenantId(customerId, tenantId);
  }

  @Override
  public CustomerMembership save(CustomerMembership membership) {
    return mapper.toDomain(repository.save(mapper.toEntity(membership)));
  }
}
