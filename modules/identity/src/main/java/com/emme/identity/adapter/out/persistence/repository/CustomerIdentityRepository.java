package com.emme.identity.adapter.out.persistence.repository;

import com.emme.identity.adapter.out.persistence.entity.CustomerIdentity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerIdentityRepository extends JpaRepository<CustomerIdentity, UUID> {
  Optional<CustomerIdentity> findByProviderAndProviderId(
      CustomerIdentity.SocialProvider provider, String providerId);
}
