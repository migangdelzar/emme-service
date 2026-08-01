package com.emme.studio.subscriptions.api.usecase;

import com.emme.studio.subscriptions.api.command.CreateSubscriptionCommand;
import com.emme.studio.subscriptions.api.result.SubscriptionInfo;

public interface CreateSubscriptionUseCase {
  SubscriptionInfo create(CreateSubscriptionCommand command);
}
