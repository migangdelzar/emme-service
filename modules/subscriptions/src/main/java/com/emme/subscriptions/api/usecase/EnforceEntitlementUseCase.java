package com.emme.subscriptions.api.usecase;

import com.emme.subscriptions.api.command.EnforceEntitlementCommand;

public interface EnforceEntitlementUseCase {
  void enforce(EnforceEntitlementCommand command);
}
