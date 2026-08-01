package com.emme.studio.application.service;

import com.emme.studio.api.usecase.UpdateBusinessProfileUseCase;
import com.emme.studio.application.port.out.BusinessProfileRepository;
import com.emme.studio.domain.model.BusinessProfile;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for updating the business profile. */
@Service
@Transactional
public class UpdateBusinessProfileService implements UpdateBusinessProfileUseCase {

  private final BusinessProfileRepository repository;

  public UpdateBusinessProfileService(BusinessProfileRepository repository) {
    this.repository = repository;
  }

  @Override
  public BusinessProfile update(UUID tenantId, String displayName, String timeZone, String locale) {
    BusinessProfile profile =
        repository
            .findByTenantId(tenantId)
            .orElse(new BusinessProfile(tenantId, timeZone, locale, displayName));
    profile.update(timeZone, locale, displayName);
    return repository.save(profile);
  }
}
