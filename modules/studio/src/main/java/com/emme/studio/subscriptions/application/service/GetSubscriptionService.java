package com.emme.studio.subscriptions.application.service;

import com.emme.studio.subscriptions.api.query.GetSubscriptionQuery;
import com.emme.studio.subscriptions.api.result.SubscriptionInfo;
import com.emme.studio.subscriptions.api.usecase.GetSubscriptionUseCase;
import com.emme.studio.subscriptions.application.mapper.SubscriptionApplicationMapper;
import com.emme.studio.subscriptions.application.port.out.SubscriptionRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetSubscriptionService implements GetSubscriptionUseCase {
  private final SubscriptionRepository repository;

  public GetSubscriptionService(SubscriptionRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<SubscriptionInfo> get(GetSubscriptionQuery query) {
    return repository.findByTenantId(query.tenantId()).map(SubscriptionApplicationMapper::toInfo);
  }
}
