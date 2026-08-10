package com.emme.identity.adapter.out.client.keycloak;

import com.emme.identity.configuration.IdentityKeycloakProperties;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import java.net.URI;
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
  private final IdentityJwtTrustPolicy trustPolicy;
  private final String jwkSetBaseUrl;

  public MultiRealmJwtDecoder(IdentityKeycloakProperties properties) {
    this.trustPolicy = new IdentityJwtTrustPolicy(properties);
    this.jwkSetBaseUrl = properties.jwkSetBaseUrl();
  }

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
    if (!trustPolicy.acceptsIssuer(issuer)) {
      throw new JwtException("JWT issuer is not trusted");
    }
    NimbusJwtDecoder decoder =
        decoders.computeIfAbsent(
            issuer,
            iss -> {
              String jwksUri = keySetUri(iss, jwkSetBaseUrl);
              NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
              jwtDecoder.setJwtValidator(trustPolicy.validatorFor(iss));
              return jwtDecoder;
            });
    return decoder.decode(token);
  }

  static String keySetUri(String issuer, String baseUrl) {
    String normalizedIssuer = stripTrailingSlash(issuer);
    if (baseUrl == null || baseUrl.isBlank()) {
      return normalizedIssuer + "/protocol/openid-connect/certs";
    }

    URI issuerUri = URI.create(normalizedIssuer);
    String path = issuerUri.getPath();
    int realmsIndex = path.indexOf("/realms/");
    if (realmsIndex < 0) {
      throw new IllegalArgumentException("Identity issuer must contain /realms/");
    }
    String realmPath = path.substring(realmsIndex);
    return stripTrailingSlash(baseUrl) + realmPath + "/protocol/openid-connect/certs";
  }

  private static String stripTrailingSlash(String value) {
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }
}
