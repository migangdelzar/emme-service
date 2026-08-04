package com.emme.e2eprovisioner.keycloak;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

class RealmDocumentFactoryTest {

  @Test
  void createsTenantOwnerRealmDocumentWithStablePublicClientAndClaims() {
    var tenantId = UUID.randomUUID();
    var configuration =
        new RealmConfiguration(
            "owner@emme.app", "password", tenantId, "e2e-studio", "http://localhost:3000");

    var document = RealmDocumentFactory.create(configuration);

    assertThat(document.path("realm").asText()).isEqualTo("emme");
    assertThat(document.path("clients").get(0).path("clientId").asText())
        .isEqualTo("emme-salon-app");
    assertThat(
            document
                .path("clients")
                .get(0)
                .path("protocolMappers")
                .get(0)
                .path("protocolMapper")
                .asText())
        .isEqualTo("oidc-audience-mapper");
    assertThat(document.path("users").get(0).path("username").asText()).isEqualTo("owner@emme.app");
    assertThat(document.path("users").get(0).path("attributes").path("tenant_id").get(0).asText())
        .isEqualTo(tenantId.toString());
    assertThat(document.path("clientScopes").get(0).path("name").asText())
        .isEqualTo("tenant-context");
    assertThat(document.path("clientScopes"))
        .extracting(node -> node.path("name").asText())
        .contains("profile", "email", "roles");
    assertThat(document.path("defaultDefaultClientScopes").isArray()).isTrue();
    var defaultScopes =
        StreamSupport.stream(document.path("defaultDefaultClientScopes").spliterator(), false)
            .map(JsonNode::asText)
            .toList();
    assertThat(defaultScopes).contains("profile", "email");
    assertThat(document.has("defaultClientScopes")).isFalse();
  }
}
