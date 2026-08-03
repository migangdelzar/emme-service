package com.emme.studio.subscriptions.api.usecase;

import com.emme.studio.subscriptions.api.command.EnforceEntitlementCommand;

public interface EnforceEntitlementUseCase {
  void enforce(EnforceEntitlementCommand command);
}
