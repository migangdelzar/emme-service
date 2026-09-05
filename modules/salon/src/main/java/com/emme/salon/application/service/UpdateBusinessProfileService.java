package com.emme.salon.application.service;

import com.emme.salon.api.result.BusinessProfileDetails;
import com.emme.salon.api.usecase.UpdateBusinessProfileUseCase;
import com.emme.salon.application.mapper.BusinessConfigurationApplicationMapper;
import com.emme.salon.application.port.out.BusinessProfileRepository;
import com.emme.salon.domain.model.BusinessProfile;
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
  public BusinessProfileDetails update(
      UUID tenantId, String displayName, String timeZone, String locale) {
    BusinessProfile profile =
        repository.find().orElse(new BusinessProfile(tenantId, timeZone, locale, displayName));
    profile.update(timeZone, locale, displayName);
    return BusinessConfigurationApplicationMapper.toDetails(repository.save(profile));
  }
}
