package com.emme.studio.application.service;

import com.emme.studio.api.result.BusinessProfileInfo;
import com.emme.studio.api.usecase.GetBusinessProfileUseCase;
import com.emme.studio.application.port.out.BusinessProfileRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for the public business-profile query. */
@Service
@Transactional(readOnly = true)
public class GetBusinessProfileService implements GetBusinessProfileUseCase {

  private final BusinessProfileRepository profileRepository;

  public GetBusinessProfileService(BusinessProfileRepository profileRepository) {
    this.profileRepository = profileRepository;
  }

  @Override
  public Optional<BusinessProfileInfo> getBusinessProfile(UUID tenantId) {
    return profileRepository
        .findByTenantId(tenantId)
        .map(p -> new BusinessProfileInfo(p.getTenantId(), p.getDisplayName(), p.getLocale()));
  }
}
