package com.emme.assistant.ai.adapter.in.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AiPrincipalIdentityTest {

  @Test
  void mapsTheTrustedIssuerAndSubjectToAStablePiiFreeUuid() {
    assertThat(AiPrincipalIdentity.fromTrustedClaims("https://issuer-a", "auth0|user-123"))
        .isEqualTo(AiPrincipalIdentity.fromTrustedClaims("https://issuer-a", "auth0|user-123"));
  }

  @Test
  void keepsSubjectsFromDifferentTrustedIssuersSeparate() {
    assertThat(AiPrincipalIdentity.fromTrustedClaims("https://issuer-a", "user-123"))
        .isNotEqualTo(AiPrincipalIdentity.fromTrustedClaims("https://issuer-b", "user-123"));
  }

  @Test
  void rejectsJwtWithoutAValidatedSubjectOrIssuer() {
    assertThatThrownBy(() -> AiPrincipalIdentity.fromTrustedClaims("", "user-123"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> AiPrincipalIdentity.fromTrustedClaims("https://issuer-a", ""))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
