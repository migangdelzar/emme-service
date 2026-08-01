package com.emme.identity.adapter.out.persistence.repository;

import com.emme.identity.adapter.out.persistence.entity.MembershipEntity;
import com.emme.identity.domain.model.MembershipStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataMembershipRepository extends JpaRepository<MembershipEntity, UUID> {
  List<MembershipEntity> findByTenantId(UUID tenantId);

  Optional<MembershipEntity> findByTenantIdAndUserReference(UUID tenantId, String userReference);

  @EntityGraph(attributePaths = {"role"})
  List<MembershipEntity> findByUserReferenceAndStatus(
      String userReference, MembershipStatus status);
}
