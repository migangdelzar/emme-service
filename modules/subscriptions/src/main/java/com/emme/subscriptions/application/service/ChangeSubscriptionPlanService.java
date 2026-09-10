package com.emme.subscriptions.application.service;

import com.emme.subscriptions.api.command.ChangeSubscriptionPlanCommand;
import com.emme.subscriptions.api.result.SubscriptionDetails;
import com.emme.subscriptions.api.usecase.ChangeSubscriptionPlanUseCase;
import com.emme.subscriptions.application.mapper.SubscriptionApplicationMapper;
import com.emme.subscriptions.application.port.out.SubscriptionRepository;
import com.emme.subscriptions.domain.model.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ChangeSubscriptionPlanService implements ChangeSubscriptionPlanUseCase {
  private final SubscriptionRepository repository;

  @Override
  public SubscriptionDetails change(ChangeSubscriptionPlanCommand command) {
    Subscription subscription =
        repository
            .find()
            .orElseThrow(() -> new IllegalArgumentException("No subscription for tenant"));
    subscription.changePlan(command.plan());
    return SubscriptionApplicationMapper.toResult(repository.save(subscription));
  }
}
