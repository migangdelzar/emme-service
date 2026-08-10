package com.emme.subscriptions.api.usecase;

import com.emme.subscriptions.api.command.CreateSubscriptionCommand;
import com.emme.subscriptions.api.result.SubscriptionDetails;

public interface CreateSubscriptionUseCase {
  SubscriptionDetails create(CreateSubscriptionCommand command);
}
