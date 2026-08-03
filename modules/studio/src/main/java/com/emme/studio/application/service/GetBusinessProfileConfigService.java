package com.emme.studio.application.service;

import com.emme.studio.api.result.BusinessProfileDetails;
import com.emme.studio.api.usecase.GetBusinessProfileConfigUseCase;
import com.emme.studio.application.mapper.BusinessConfigurationApplicationMapper;
import com.emme.studio.application.port.out.BusinessProfileRepository;
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
  public Optional<BusinessProfileDetails> get(UUID tenantId) {
    return repository
        .findByTenantId(tenantId)
        .map(BusinessConfigurationApplicationMapper::toDetails);
  }
}
