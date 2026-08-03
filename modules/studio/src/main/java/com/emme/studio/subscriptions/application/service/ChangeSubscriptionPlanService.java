package com.emme.studio.subscriptions.application.service;

import com.emme.studio.subscriptions.api.command.ChangeSubscriptionPlanCommand;
import com.emme.studio.subscriptions.api.result.SubscriptionInfo;
import com.emme.studio.subscriptions.api.usecase.ChangeSubscriptionPlanUseCase;
import com.emme.studio.subscriptions.application.mapper.SubscriptionApplicationMapper;
import com.emme.studio.subscriptions.application.port.out.SubscriptionRepository;
import com.emme.studio.subscriptions.domain.model.Subscription;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChangeSubscriptionPlanService implements ChangeSubscriptionPlanUseCase {
  private final SubscriptionRepository repository;

  public ChangeSubscriptionPlanService(SubscriptionRepository repository) {
    this.repository = repository;
  }

  @Override
  public SubscriptionInfo change(ChangeSubscriptionPlanCommand command) {
    Subscription subscription =
        repository
            .findByTenantId(command.tenantId())
            .orElseThrow(() -> new IllegalArgumentException("No subscription for tenant"));
    subscription.changePlan(command.plan());
    return SubscriptionApplicationMapper.toInfo(repository.save(subscription));
  }
}
