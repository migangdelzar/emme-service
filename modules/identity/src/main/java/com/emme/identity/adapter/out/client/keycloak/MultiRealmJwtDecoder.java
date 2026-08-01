package com.emme.identity.adapter.out.client.keycloak;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Dynamic JWT decoder that resolves the JWKS URI from the JWT's "iss" claim. Each Keycloak realm
 * has its own certs endpoint.
 */
@Component
public class MultiRealmJwtDecoder implements JwtDecoder {

  private final Map<String, NimbusJwtDecoder> decoders = new ConcurrentHashMap<>();

  @Override
  public Jwt decode(String token) throws JwtException {
    String issuer;
    try {
      JWT jwt = JWTParser.parse(token);
      issuer = jwt.getJWTClaimsSet().getIssuer();
    } catch (ParseException e) {
      throw new JwtException("Cannot parse JWT issuer", e);
    }
    if (issuer == null) {
      throw new JwtException("JWT missing 'iss' claim");
    }
    NimbusJwtDecoder decoder =
        decoders.computeIfAbsent(
            issuer,
            iss -> {
              String jwksUri = iss + "/protocol/openid-connect/certs";
              return NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
            });
    return decoder.decode(token);
  }
}
