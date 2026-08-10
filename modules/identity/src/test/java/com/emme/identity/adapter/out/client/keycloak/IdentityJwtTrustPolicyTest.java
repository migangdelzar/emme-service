package com.emme.identity.adapter.out.client.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emme.identity.configuration.IdentityKeycloakProperties;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class IdentityJwtTrustPolicyTest {

  private final IdentityKeycloakProperties properties = configuredProperties();
  private final IdentityJwtTrustPolicy policy = new IdentityJwtTrustPolicy(properties);

  @Test
  void acceptsTheConfiguredPlatformIssuer() {
    assertThat(policy.acceptsIssuer("https://identity.example/realms/emme-core")).isTrue();
  }

  @Test
  void acceptsOnlyTenantIssuersUsingTheConfiguredRealmNamingPattern() {
    assertThat(policy.acceptsIssuer("https://identity.example/realms/emme-demo-salon")).isTrue();
    assertThat(policy.acceptsIssuer("https://identity.example/realms/emme-customers")).isTrue();
    assertThat(policy.acceptsIssuer("https://identity.example/realms/emmeevil")).isFalse();
    assertThat(policy.acceptsIssuer("https://identity.example/realms/emme-")).isFalse();
    assertThat(policy.acceptsIssuer("https://identity.example/realms/emme-demo-salon/path"))
        .isFalse();
    assertThat(policy.acceptsIssuer("https://attacker.example/realms/emme-demo-salon")).isFalse();
  }

  @Test
  void rejectsAnIssuerBeforeAKeySetEndpointCanBeResolved() {
    assertThatThrownBy(() -> policy.validatorFor("https://attacker.example/realms/emme-demo-salon"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("JWT issuer is not trusted");
  }

  @Test
  void requiresTheConfiguredAudience() {
    Jwt validJwt = jwt(List.of("admin-app"));
    Jwt invalidJwt = jwt(List.of("another-client"));

    assertThat(policy.validatorFor(validJwt.getIssuer().toString()).validate(validJwt).hasErrors())
        .isFalse();
    assertThat(
            policy.validatorFor(invalidJwt.getIssuer().toString()).validate(invalidJwt).hasErrors())
        .isTrue();
  }

  @Test
  void keepsTheCoreRealmOnThePlatformAudienceWhenItsNameUsesTheTenantPrefix() {
    Jwt coreJwt = jwt("https://identity.example/realms/emme-core", List.of("admin-app"));

    assertThat(policy.validatorFor(coreJwt.getIssuer().toString()).validate(coreJwt).hasErrors())
        .isFalse();
  }

  @Test
  void usesTheCustomerAudienceForTheDedicatedCustomerRealm() {
    Jwt customerJwt =
        jwt("https://identity.example/realms/emme-customers", List.of("emme-customer-app"));

    assertThat(
            policy
                .validatorFor(customerJwt.getIssuer().toString())
                .validate(customerJwt)
                .hasErrors())
        .isFalse();
  }

  @Test
  void usesTheSalonAudienceForTenantRealms() {
    Jwt salonJwt = jwt("https://identity.example/realms/emme-demo-salon", List.of("salon-app"));

    assertThat(policy.validatorFor(salonJwt.getIssuer().toString()).validate(salonJwt).hasErrors())
        .isFalse();
  }

  private static IdentityKeycloakProperties configuredProperties() {
    return new IdentityKeycloakProperties(
        "https://identity.example",
        "https://identity.example/realms/emme-core",
        "",
        "salon-app",
        "admin-app",
        "master",
        "admin",
        "",
        "emme-core",
        "https://identity.example/realms/emme-customers",
        "emme-customer-app");
  }

  private static Jwt jwt(List<String> audiences) {
    return jwt("https://identity.example/realms/emme-core", audiences);
  }

  private static Jwt jwt(String issuer, List<String> audiences) {
    return Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .issuer(issuer)
        .subject("user-1")
        .audience(audiences)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .build();
  }
}
