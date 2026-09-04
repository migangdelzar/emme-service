package com.emme.salon.adapter.out.persistence.adapter;

import com.emme.salon.adapter.out.persistence.entity.BusinessProfileEntity;
import com.emme.salon.adapter.out.persistence.mapper.BusinessProfilePersistenceMapper;
import com.emme.salon.adapter.out.persistence.repository.SpringDataBusinessProfileRepository;
import com.emme.salon.application.port.out.BusinessProfileRepository;
import com.emme.salon.domain.model.BusinessProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implements the business-profile persistence port using Spring Data JPA. */
@Component
public class BusinessProfilePersistenceAdapter implements BusinessProfileRepository {

  private final SpringDataBusinessProfileRepository repository;
  private final BusinessProfilePersistenceMapper mapper;

  public BusinessProfilePersistenceAdapter(SpringDataBusinessProfileRepository repository) {
    this.repository = repository;
    this.mapper = new BusinessProfilePersistenceMapper();
  }

  @Override
  public BusinessProfile save(BusinessProfile profile) {
    BusinessProfileEntity entity =
        profile.getId() == null
            ? mapper.toNewEntity(profile)
            : repository.findByTenantIdAndId(profile.getTenantId(), profile.getId()).orElseThrow();
    mapper.updateEntity(profile, entity);
    return mapper.toDomain(repository.save(entity));
  }

  @Override
  public Optional<BusinessProfile> findByTenantId(UUID tenantId) {
    return repository.findByTenantId(tenantId).map(mapper::toDomain);
  }
}
