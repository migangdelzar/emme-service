package com.emme.identity.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CustomerIdentityRepository extends JpaRepository<CustomerIdentity, UUID> {
    Optional<CustomerIdentity> findByProviderAndProviderId(
        CustomerIdentity.SocialProvider provider, String providerId);
}
