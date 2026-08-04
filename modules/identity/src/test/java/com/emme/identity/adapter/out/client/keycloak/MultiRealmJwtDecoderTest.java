package com.emme.identity.adapter.out.client.keycloak;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MultiRealmJwtDecoderTest {

  @Test
  void resolvesInternalKeySetEndpointFromExternalIssuer() {
    assertThat(
            MultiRealmJwtDecoder.keySetUri(
                "http://127.0.0.1:18080/realms/emme-demo", "http://keycloak:8080"))
        .isEqualTo("http://keycloak:8080/realms/emme-demo/protocol/openid-connect/certs");
  }
}
