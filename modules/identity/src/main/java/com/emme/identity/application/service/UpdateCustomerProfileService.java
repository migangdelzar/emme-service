package com.emme.identity.application.service;

import com.emme.identity.api.command.UpdateCustomerPhoneCommand;
import com.emme.identity.api.result.CustomerDetails;
import com.emme.identity.api.usecase.UpdateCustomerProfileUseCase;
import com.emme.identity.application.port.out.CustomerIdentityRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates customer profile updates through the Identity application boundary. */
@Service
@Transactional
public class UpdateCustomerProfileService implements UpdateCustomerProfileUseCase {

  private final CustomerIdentityRepository repository;

  public UpdateCustomerProfileService(CustomerIdentityRepository repository) {
    this.repository = repository;
  }

  @Override
  public CustomerDetails updatePhone(UpdateCustomerPhoneCommand command) {
    UUID customerId = command.customerId();
    var customer =
        repository
            .findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
    customer.updatePhone(command.phone());
    return AuthenticateCustomerService.toDetails(repository.save(customer));
  }
}
