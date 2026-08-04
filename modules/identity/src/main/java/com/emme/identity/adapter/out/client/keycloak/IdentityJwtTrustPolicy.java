package com.emme.identity.adapter.out.client.keycloak;

import com.emme.identity.configuration.IdentityKeycloakProperties;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;

/**
 * Defines which Keycloak issuers and audience are trusted by Identity.
 *
 * <p>The issuer is checked before a dynamic JWKS URL is constructed. This prevents an untrusted
 * token from turning its {@code iss} claim into an outbound discovery request.
 */
public final class IdentityJwtTrustPolicy {

  private final String platformIssuer;
  private final String customerIssuer;
  private final String tenantIssuerPrefix;
  private final String platformAudience;
  private final String customerAudience;

  public IdentityJwtTrustPolicy(IdentityKeycloakProperties properties) {
    this.platformIssuer = normalizeIssuer(properties.issuerUri());
    this.customerIssuer = normalizeIssuer(properties.customerIssuerUri());
    this.tenantIssuerPrefix = tenantIssuerPrefix(platformIssuer);
    this.platformAudience = properties.clientId();
    this.customerAudience = properties.customerClientId();
  }

  public boolean acceptsIssuer(String issuer) {
    String normalizedIssuer = normalizeIssuer(issuer);
    return platformIssuer.equals(normalizedIssuer)
        || customerIssuer.equals(normalizedIssuer)
        || (normalizedIssuer.startsWith(tenantIssuerPrefix)
            && normalizedIssuer.length() > tenantIssuerPrefix.length()
            && !normalizedIssuer.substring(tenantIssuerPrefix.length()).contains("/"));
  }

  public OAuth2TokenValidator<Jwt> validatorFor(String issuer) {
    if (!acceptsIssuer(issuer)) {
      throw new IllegalArgumentException("JWT issuer is not trusted");
    }
    String normalizedIssuer = normalizeIssuer(issuer);
    String audience = customerIssuer.equals(normalizedIssuer) ? customerAudience : platformAudience;
    return new DelegatingOAuth2TokenValidator<>(
        JwtValidators.createDefaultWithIssuer(normalizedIssuer),
        new JwtAudienceValidator(audience));
  }

  private static String normalizeIssuer(String issuer) {
    if (issuer == null || issuer.isBlank()) {
      return "";
    }
    return issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
  }

  private static String tenantIssuerPrefix(String issuer) {
    int realmSeparator = issuer.lastIndexOf("/realms/");
    if (realmSeparator < 0) {
      throw new IllegalArgumentException("Identity issuer must contain /realms/");
    }
    return issuer.substring(0, realmSeparator + "/realms/".length())
        + issuer.substring(realmSeparator + "/realms/".length())
        + "-";
  }
}
