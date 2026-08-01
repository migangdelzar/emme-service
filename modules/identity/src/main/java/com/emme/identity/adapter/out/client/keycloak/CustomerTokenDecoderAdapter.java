package com.emme.identity.adapter.out.client.keycloak;

import com.emme.identity.application.port.out.CustomerTokenClaims;
import com.emme.identity.application.port.out.CustomerTokenDecoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Adapts the Keycloak JWT decoder to the customer authentication port. */
@Component
public final class CustomerTokenDecoderAdapter implements CustomerTokenDecoder {

  private final MultiRealmJwtDecoder decoder;

  public CustomerTokenDecoderAdapter(MultiRealmJwtDecoder decoder) {
    this.decoder = decoder;
  }

  @Override
  public CustomerTokenClaims decode(String providerToken) {
    Jwt jwt = decoder.decode(providerToken);
    return new CustomerTokenClaims(
        jwt.getIssuer() == null ? null : jwt.getIssuer().toString(),
        jwt.getSubject(),
        jwt.getClaimAsString("email"),
        jwt.getClaimAsString("name"),
        jwt.getClaimAsString("identity_provider"),
        jwt.getClaimAsString("picture"));
  }
}
