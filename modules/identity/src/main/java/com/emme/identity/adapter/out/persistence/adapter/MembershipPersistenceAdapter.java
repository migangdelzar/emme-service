package com.emme.identity.adapter.out.persistence.adapter;

import com.emme.identity.adapter.out.persistence.entity.MembershipEntity;
import com.emme.identity.adapter.out.persistence.entity.Role;
import com.emme.identity.adapter.out.persistence.mapper.MembershipPersistenceMapper;
import com.emme.identity.adapter.out.persistence.repository.SpringDataMembershipRepository;
import com.emme.identity.adapter.out.persistence.repository.SpringDataRoleRepository;
import com.emme.identity.application.port.out.MembershipRepository;
import com.emme.identity.domain.model.Membership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements the Membership repository port with Spring Data JPA. */
@Component
public final class MembershipPersistenceAdapter implements MembershipRepository {

  private final SpringDataMembershipRepository repository;
  private final SpringDataRoleRepository roleRepository;
  private final MembershipPersistenceMapper mapper;

  public MembershipPersistenceAdapter(
      SpringDataMembershipRepository repository,
      SpringDataRoleRepository roleRepository,
      MembershipPersistenceMapper mapper) {
    this.repository = repository;
    this.roleRepository = roleRepository;
    this.mapper = mapper;
  }

  @Override
  public Membership save(Membership membership) {
    Role role =
        roleRepository
            .findById(membership.roleId())
            .orElseThrow(
                () -> new IllegalArgumentException("Role not found: " + membership.roleId()));
    MembershipEntity entity =
        membership.id() == null
            ? mapper.toEntity(membership, role)
            : repository
                .findById(membership.id())
                .map(existing -> update(existing, membership))
                .orElseGet(() -> mapper.toEntity(membership, role));
    return mapper.toDomain(repository.save(entity));
  }

  @Override
  public Optional<Membership> findById(UUID membershipId) {
    return repository.findById(membershipId).map(mapper::toDomain);
  }

  @Override
  public List<Membership> findByUserReference(String userReference) {
    return repository.findByUserReference(userReference).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Membership> findActiveByUserReference(String userReference) {
    return repository
        .findByUserReferenceAndStatus(
            userReference, com.emme.identity.domain.model.MembershipStatus.ACTIVE)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  private MembershipEntity update(MembershipEntity entity, Membership membership) {
    entity.setStatus(membership.status());
    return entity;
  }
}
