package com.emme.identity.application.port.out;

import com.emme.identity.domain.model.CustomerIdentity;
import com.emme.identity.domain.model.SocialProvider;
import java.util.Optional;
import java.util.UUID;

/** Persistence capability required by customer identity use cases. */
public interface CustomerIdentityRepository {

  Optional<CustomerIdentity> findById(UUID customerId);

  Optional<CustomerIdentity> findByProviderAndProviderId(
      SocialProvider provider, String providerId);

  CustomerIdentity save(CustomerIdentity customer);
}
