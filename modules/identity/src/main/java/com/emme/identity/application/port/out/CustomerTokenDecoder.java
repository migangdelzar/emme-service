package com.emme.identity.application.port.out;

/** External capability that verifies and decodes a customer provider token. */
public interface CustomerTokenDecoder {

  CustomerTokenClaims decode(String providerToken);
}
