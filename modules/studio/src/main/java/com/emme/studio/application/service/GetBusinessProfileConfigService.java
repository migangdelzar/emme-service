package com.emme.studio.application.service;

import com.emme.studio.api.usecase.GetBusinessProfileConfigUseCase;
import com.emme.studio.application.port.out.BusinessProfileRepository;
import com.emme.studio.domain.model.BusinessProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for retrieving the editable business profile. */
@Service
@Transactional(readOnly = true)
public class GetBusinessProfileConfigService implements GetBusinessProfileConfigUseCase {

  private final BusinessProfileRepository repository;

  public GetBusinessProfileConfigService(BusinessProfileRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<BusinessProfile> get(UUID tenantId) {
    return repository.findByTenantId(tenantId);
  }
}
