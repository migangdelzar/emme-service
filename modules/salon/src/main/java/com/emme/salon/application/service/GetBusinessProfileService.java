package com.emme.salon.application.service;

import com.emme.salon.api.result.BusinessProfileSummary;
import com.emme.salon.api.usecase.GetBusinessProfileUseCase;
import com.emme.salon.application.port.out.BusinessProfileRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for the public business-profile query. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetBusinessProfileService implements GetBusinessProfileUseCase {

  private final BusinessProfileRepository profileRepository;

  @Override
  public Optional<BusinessProfileSummary> getBusinessProfile(UUID tenantId) {
    return profileRepository
        .find()
        .map(p -> new BusinessProfileSummary(p.getTenantId(), p.getDisplayName(), p.getLocale()));
  }
}
