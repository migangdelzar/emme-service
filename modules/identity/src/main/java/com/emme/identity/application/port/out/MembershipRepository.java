package com.emme.identity.application.port.out;

import com.emme.identity.domain.model.Membership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence capability required by Identity membership use cases. */
public interface MembershipRepository {

  Membership save(Membership membership);

  Optional<Membership> findByIdInTenant(UUID membershipId, UUID tenantId);

  List<Membership> findActiveByUserReference(String userReference);
}
