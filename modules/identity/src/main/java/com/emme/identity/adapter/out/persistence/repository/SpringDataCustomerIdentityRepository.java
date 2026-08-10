package com.emme.identity.adapter.out.persistence.repository;

import com.emme.identity.adapter.out.persistence.entity.CustomerIdentityEntity;
import com.emme.identity.domain.model.SocialProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data mechanics for customer identity persistence. */
public interface SpringDataCustomerIdentityRepository
    extends JpaRepository<CustomerIdentityEntity, UUID> {

  Optional<CustomerIdentityEntity> findByProviderAndProviderId(
      SocialProvider provider, String providerId);
}
