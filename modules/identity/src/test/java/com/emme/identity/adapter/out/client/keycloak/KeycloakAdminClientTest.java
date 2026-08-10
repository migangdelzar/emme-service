package com.emme.identity.adapter.out.client.keycloak;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class KeycloakAdminClientTest {

  @Test
  void createsAClientWithItsOwnAccessTokenAudience() throws Exception {
    var representation =
        KeycloakAdminClient.clientRepresentation("salon-app", List.of("http://localhost:3000/*"));

    var json = new ObjectMapper().valueToTree(representation);

    assertThat(json.path("clientId").asText()).isEqualTo("salon-app");
    assertThat(json.path("protocolMappers").findValuesAsText("protocolMapper"))
        .contains("oidc-audience-mapper");
    assertThat(json.path("protocolMappers").toString()).contains("salon-app");
  }
}
