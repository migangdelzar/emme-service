package com.emme.identity.adapter.out.persistence.repository;

import com.emme.identity.adapter.out.persistence.entity.Membership;
import com.emme.identity.adapter.out.persistence.entity.MembershipStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {
  List<Membership> findByTenantId(UUID tenantId);

  Optional<Membership> findByTenantIdAndUserReference(UUID tenantId, String userReference);

  @EntityGraph(attributePaths = {"role"})
  List<Membership> findByUserReferenceAndStatus(String userReference, MembershipStatus status);
}
