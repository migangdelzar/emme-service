package com.emme.identity.api.usecase;

import com.emme.identity.api.command.AuthenticateCustomerCommand;
import com.emme.identity.api.result.CustomerLoginResult;

/** Public customer authentication capability exposed by Identity. */
public interface AuthenticateCustomerUseCase {

  CustomerLoginResult authenticate(AuthenticateCustomerCommand command);
}
