package com.emme.identity.adapter.out.persistence.adapter;

import com.emme.identity.adapter.out.persistence.mapper.CustomerIdentityPersistenceMapper;
import com.emme.identity.adapter.out.persistence.repository.SpringDataCustomerIdentityRepository;
import com.emme.identity.application.port.out.CustomerIdentityRepository;
import com.emme.identity.domain.model.CustomerIdentity;
import com.emme.identity.domain.model.SocialProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements customer identity persistence with Spring Data JPA. */
@Component
public final class CustomerIdentityPersistenceAdapter implements CustomerIdentityRepository {

  private final SpringDataCustomerIdentityRepository repository;
  private final CustomerIdentityPersistenceMapper mapper;

  public CustomerIdentityPersistenceAdapter(
      SpringDataCustomerIdentityRepository repository, CustomerIdentityPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Optional<CustomerIdentity> findById(UUID customerId) {
    return repository.findById(customerId).map(mapper::toDomain);
  }

  @Override
  public Optional<CustomerIdentity> findByProviderAndProviderId(
      SocialProvider provider, String providerId) {
    return repository.findByProviderAndProviderId(provider, providerId).map(mapper::toDomain);
  }

  @Override
  public CustomerIdentity save(CustomerIdentity customer) {
    return mapper.toDomain(repository.save(mapper.toEntity(customer)));
  }
}
