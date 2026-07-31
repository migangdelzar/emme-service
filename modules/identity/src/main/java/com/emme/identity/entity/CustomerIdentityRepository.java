package com.emme.identity.entity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerIdentityRepository extends JpaRepository<CustomerIdentity, UUID> {
  Optional<CustomerIdentity> findByProviderAndProviderId(
      CustomerIdentity.SocialProvider provider, String providerId);
}
