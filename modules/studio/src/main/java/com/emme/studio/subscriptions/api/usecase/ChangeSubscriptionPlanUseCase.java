package com.emme.studio.subscriptions.api.usecase;

import com.emme.studio.subscriptions.api.command.ChangeSubscriptionPlanCommand;
import com.emme.studio.subscriptions.api.result.SubscriptionInfo;

public interface ChangeSubscriptionPlanUseCase {
  SubscriptionInfo change(ChangeSubscriptionPlanCommand command);
}
