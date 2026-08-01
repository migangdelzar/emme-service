package com.emme.identity.application;

import com.emme.identity.entity.CustomerIdentity;
import com.emme.identity.entity.CustomerIdentity.SocialProvider;
import com.emme.identity.entity.CustomerIdentityRepository;
import com.emme.identity.infrastructure.MultiRealmJwtDecoder;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles customer authentication via social login tokens from the emme-customers Keycloak realm.
 * Creates or updates global customer identity. Does NOT handle staff or platform login.
 */
@Service
public class CustomerAuthService {

  private static final Logger log = LoggerFactory.getLogger(CustomerAuthService.class);
  private static final String CUSTOMERS_ISSUER_SUFFIX = "/realms/emme-customers";

  private final CustomerIdentityRepository customerRepo;
  private final MultiRealmJwtDecoder jwtDecoder;

  public CustomerAuthService(
      CustomerIdentityRepository customerRepo, MultiRealmJwtDecoder jwtDecoder) {
    this.customerRepo = customerRepo;
    this.jwtDecoder = jwtDecoder;
  }

  /**
   * Validates a provider token from the emme-customers realm and returns or creates the customer
   * identity record.
   */
  @Transactional
  public CustomerLoginResult authenticate(String providerToken) {
    Jwt jwt = jwtDecoder.decode(providerToken);
    var issuerUri = jwt.getIssuer();
    if (issuerUri == null || !issuerUri.toString().contains(CUSTOMERS_ISSUER_SUFFIX)) {
      throw new IllegalArgumentException("Token not from customers realm");
    }

    String email = jwt.getClaimAsString("email");
    String name = jwt.getClaimAsString("name");
    String providerStr = jwt.getClaimAsString("identity_provider");
    String providerId = jwt.getClaimAsString("sub");
    String avatarUrl = jwt.getClaimAsString("picture");

    if (providerStr == null) {
      // Fallback: try "azp" or "iss" claim for provider
      providerStr = "GOOGLE";
    }
    SocialProvider provider;
    try {
      provider = SocialProvider.valueOf(providerStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("Unknown provider '{}', defaulting to GOOGLE", providerStr);
      provider = SocialProvider.GOOGLE;
    }

    SocialProvider finalProvider = provider;
    CustomerIdentity customer =
        customerRepo
            .findByProviderAndProviderId(provider, providerId)
            .orElseGet(
                () -> {
                  var c = new CustomerIdentity();
                  c.setEmail(email);
                  c.setName(name);
                  c.setProvider(finalProvider);
                  c.setProviderId(providerId);
                  c.setAvatarUrl(avatarUrl);
                  return customerRepo.save(c);
                });

    // Update name/avatar if changed
    boolean changed = false;
    if (name != null && !name.equals(customer.getName())) {
      customer.setName(name);
      changed = true;
    }
    if (avatarUrl != null && !avatarUrl.equals(customer.getAvatarUrl())) {
      customer.setAvatarUrl(avatarUrl);
      changed = true;
    }
    if (changed) customerRepo.save(customer);

    boolean needsPhone = customer.getPhone() == null || customer.getPhone().isBlank();

    return new CustomerLoginResult(customer, needsPhone);
  }

  @Transactional
  public CustomerIdentity updatePhone(UUID customerId, String phone) {
    var c =
        customerRepo
            .findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
    c.setPhone(phone);
    c.setUpdatedAt(java.time.Instant.now());
    return customerRepo.save(c);
  }

  public record CustomerLoginResult(CustomerIdentity customer, boolean needsPhone) {}
}
