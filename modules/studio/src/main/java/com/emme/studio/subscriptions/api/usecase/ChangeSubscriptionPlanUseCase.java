package com.emme.studio.subscriptions.api.usecase;

import com.emme.studio.subscriptions.api.command.ChangeSubscriptionPlanCommand;
import com.emme.studio.subscriptions.api.result.SubscriptionDetails;

public interface ChangeSubscriptionPlanUseCase {
  SubscriptionDetails change(ChangeSubscriptionPlanCommand command);
}
