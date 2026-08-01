package com.emme.identity.api.usecase;

import com.emme.identity.api.command.UpdateCustomerPhoneCommand;
import com.emme.identity.api.result.CustomerDetails;

/** Public customer profile update capability exposed by Identity. */
public interface UpdateCustomerProfileUseCase {

  CustomerDetails updatePhone(UpdateCustomerPhoneCommand command);
}
