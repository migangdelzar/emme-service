package com.emme.identity.application.service;

import com.emme.identity.api.command.AuthenticateCustomerCommand;
import com.emme.identity.api.result.CustomerDetails;
import com.emme.identity.api.result.CustomerLoginResult;
import com.emme.identity.api.usecase.AuthenticateCustomerUseCase;
import com.emme.identity.application.port.out.CustomerIdentityRepository;
import com.emme.identity.application.port.out.CustomerTokenClaims;
import com.emme.identity.application.port.out.CustomerTokenDecoder;
import com.emme.identity.domain.model.CustomerIdentity;
import com.emme.identity.domain.model.SocialProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Authenticates customer provider tokens and coordinates identity persistence. */
@Service
@Transactional
public class AuthenticateCustomerService implements AuthenticateCustomerUseCase {

  private static final Logger log = LoggerFactory.getLogger(AuthenticateCustomerService.class);
  private static final String CUSTOMERS_ISSUER_SUFFIX = "/realms/emme-customers";

  private final CustomerIdentityRepository repository;
  private final CustomerTokenDecoder tokenDecoder;

  public AuthenticateCustomerService(
      CustomerIdentityRepository repository, CustomerTokenDecoder tokenDecoder) {
    this.repository = repository;
    this.tokenDecoder = tokenDecoder;
  }

  @Override
  public CustomerLoginResult authenticate(AuthenticateCustomerCommand command) {
    CustomerTokenClaims claims = tokenDecoder.decode(command.providerToken());
    if (claims.issuer() == null || !claims.issuer().contains(CUSTOMERS_ISSUER_SUFFIX)) {
      throw new IllegalArgumentException("Token not from customers realm");
    }

    SocialProvider provider = resolveProvider(claims.identityProvider());
    CustomerIdentity customer =
        repository
            .findByProviderAndProviderId(provider, claims.subject())
            .orElseGet(
                () ->
                    repository.save(
                        CustomerIdentity.create(
                            claims.email(),
                            claims.name(),
                            provider,
                            claims.subject(),
                            claims.avatarUrl())));
    if (customer.updateProfile(claims.name(), claims.avatarUrl())) {
      customer = repository.save(customer);
    }

    return new CustomerLoginResult(toDetails(customer), customer.needsPhone());
  }

  private SocialProvider resolveProvider(String providerValue) {
    String value = providerValue == null ? "GOOGLE" : providerValue;
    try {
      return SocialProvider.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException exception) {
      log.warn("Unknown provider '{}', defaulting to GOOGLE", value);
      return SocialProvider.GOOGLE;
    }
  }

  static CustomerDetails toDetails(CustomerIdentity customer) {
    return new CustomerDetails(
        customer.id(),
        customer.email(),
        customer.name(),
        customer.phone(),
        customer.provider().name());
  }
}
