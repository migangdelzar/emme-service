package com.emme.identity.adapter.out.persistence.adapter;

import com.emme.identity.adapter.out.persistence.repository.SpringDataCustomerMembershipRepository;
import com.emme.identity.application.port.out.CustomerMembershipRepository;
import com.emme.identity.domain.model.CustomerMembership;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements customer membership persistence with Spring Data JPA. */
@Component
public class CustomerMembershipPersistenceAdapter implements CustomerMembershipRepository {

  private final SpringDataCustomerMembershipRepository repository;

  public CustomerMembershipPersistenceAdapter(SpringDataCustomerMembershipRepository repository) {
    this.repository = repository;
  }

  @Override
  public boolean existsByCustomerIdAndTenantId(UUID customerId, UUID tenantId) {
    return repository.existsByCustomerIdAndTenantId(customerId, tenantId);
  }

  @Override
  public boolean createIfAbsent(CustomerMembership membership) {
    return repository.insertIfAbsent(
            membership.customerId(), membership.tenantId(), membership.createdAt())
        == 1;
  }
}
