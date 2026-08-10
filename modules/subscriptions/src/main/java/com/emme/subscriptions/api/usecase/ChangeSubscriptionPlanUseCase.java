package com.emme.subscriptions.api.usecase;

import com.emme.subscriptions.api.command.ChangeSubscriptionPlanCommand;
import com.emme.subscriptions.api.result.SubscriptionDetails;

public interface ChangeSubscriptionPlanUseCase {
  SubscriptionDetails change(ChangeSubscriptionPlanCommand command);
}
