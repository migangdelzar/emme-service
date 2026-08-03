package com.emme.studio.application.service;

import com.emme.studio.api.result.BookingPolicyDetails;
import com.emme.studio.api.usecase.UpdateBookingPolicyUseCase;
import com.emme.studio.application.mapper.BusinessConfigurationApplicationMapper;
import com.emme.studio.application.port.out.BookingPolicyRepository;
import com.emme.studio.domain.model.BookingPolicy;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for updating booking policy. */
@Service
@Transactional
public class UpdateBookingPolicyService implements UpdateBookingPolicyUseCase {

  private final BookingPolicyRepository repository;

  public UpdateBookingPolicyService(BookingPolicyRepository repository) {
    this.repository = repository;
  }

  @Override
  public BookingPolicyDetails update(
      UUID tenantId, int minNotice, int maxAdvance, int cancelWindow, boolean allowOverlap) {
    BookingPolicy policy =
        repository
            .findByTenantId(tenantId)
            .orElse(new BookingPolicy(tenantId, minNotice, maxAdvance, cancelWindow, allowOverlap));
    policy.update(minNotice, maxAdvance, cancelWindow, allowOverlap);
    return BusinessConfigurationApplicationMapper.toDetails(repository.save(policy));
  }
}
