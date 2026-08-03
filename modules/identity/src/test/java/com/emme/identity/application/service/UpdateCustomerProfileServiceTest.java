package com.emme.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.emme.identity.api.command.UpdateCustomerPhoneCommand;
import com.emme.identity.application.port.out.CustomerIdentityRepository;
import com.emme.identity.domain.model.CustomerIdentity;
import com.emme.identity.domain.model.SocialProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateCustomerProfileServiceTest {

  @Mock private CustomerIdentityRepository repository;

  @Test
  void updatesPhoneThroughTheApplicationOwnedRepositoryPort() {
    UUID customerId = UUID.randomUUID();
    CustomerIdentity customer =
        CustomerIdentity.create(
            "customer@example.com", "Customer", SocialProvider.GOOGLE, "p-1", null);
    customer =
        CustomerIdentity.rehydrate(
            customerId,
            customer.email(),
            customer.name(),
            null,
            customer.provider(),
            customer.providerId(),
            customer.avatarUrl(),
            customer.createdAt(),
            customer.updatedAt());
    when(repository.findById(customerId)).thenReturn(Optional.of(customer));
    when(repository.save(any(CustomerIdentity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result =
        new UpdateCustomerProfileService(repository)
            .updatePhone(new UpdateCustomerPhoneCommand(customerId, "+5215550101"));

    assertThat(result.phone()).isEqualTo("+5215550101");
  }
}
