package com.emme.studio.subscriptions.api.usecase;

import com.emme.studio.subscriptions.api.command.CreateSubscriptionCommand;
import com.emme.studio.subscriptions.api.result.SubscriptionDetails;

public interface CreateSubscriptionUseCase {
  SubscriptionDetails create(CreateSubscriptionCommand command);
}
