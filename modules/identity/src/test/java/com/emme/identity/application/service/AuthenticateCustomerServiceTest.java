package com.emme.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.emme.identity.api.command.AuthenticateCustomerCommand;
import com.emme.identity.api.result.CustomerLoginResult;
import com.emme.identity.application.port.out.CustomerIdentityRepository;
import com.emme.identity.application.port.out.CustomerTokenClaims;
import com.emme.identity.application.port.out.CustomerTokenDecoder;
import com.emme.identity.domain.model.CustomerIdentity;
import com.emme.identity.domain.model.SocialProvider;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticateCustomerServiceTest {

  @Mock private CustomerIdentityRepository repository;
  @Mock private CustomerTokenDecoder tokenDecoder;

  @Test
  void createsCustomerFromVerifiedProviderClaimsAndReturnsApiResult() {
    when(tokenDecoder.decode("provider-token"))
        .thenReturn(
            new CustomerTokenClaims(
                "https://auth.example/realms/emme-customers",
                "provider-1",
                "customer@example.com",
                "Customer",
                "GOOGLE",
                "avatar"));
    when(repository.findByProviderAndProviderId(SocialProvider.GOOGLE, "provider-1"))
        .thenReturn(Optional.empty());
    when(repository.save(any(CustomerIdentity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CustomerLoginResult result =
        new AuthenticateCustomerService(repository, tokenDecoder)
            .authenticate(new AuthenticateCustomerCommand("provider-token"));

    assertThat(result.needsPhone()).isTrue();
    assertThat(result.customer().email()).isEqualTo("customer@example.com");
    assertThat(result.customer().provider()).isEqualTo("GOOGLE");
  }

  @Test
  void rejectsTokensFromAnotherKeycloakRealm() {
    when(tokenDecoder.decode("provider-token"))
        .thenReturn(
            new CustomerTokenClaims(
                "https://auth.example/realms/emme",
                "provider-1",
                "customer@example.com",
                "Customer",
                "GOOGLE",
                null));

    assertThatThrownBy(
            () ->
                new AuthenticateCustomerService(repository, tokenDecoder)
                    .authenticate(new AuthenticateCustomerCommand("provider-token")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Token not from customers realm");
  }
}
