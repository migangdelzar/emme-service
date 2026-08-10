package com.emme.e2eprovisioner;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class E2eTemplateTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void importsThePlatformRealmAndBootstrapAdmin() throws IOException {
    JsonNode realm = objectMapper.readTree(resource("templates/keycloak/realm.json"));

    assertThat(realm.path("realm").asText()).isEqualTo("emme-core");
    assertThat(realm.path("roles").path("realm").findValuesAsText("name")).containsExactly("admin");
    assertThat(realm.path("clients").findValuesAsText("clientId"))
        .contains("admin-app")
        .doesNotContain("emme-salon-app");
    assertThat(realm.path("users").findValuesAsText("username")).contains("admin@emme-core.local");

    JsonNode admin = realm.path("users").get(0);
    assertThat(admin.path("firstName").asText()).isNotBlank();
    assertThat(admin.path("lastName").asText()).isNotBlank();
    assertThat(admin.path("credentials").get(0).path("temporary").asBoolean()).isFalse();
  }

  @Test
  void importsTheSharedCustomerRealmForSocialLogin() throws IOException {
    JsonNode realm = objectMapper.readTree(resource("templates/keycloak/customers-realm.json"));

    assertThat(realm.path("realm").asText()).isEqualTo("emme-customers");
    assertThat(realm.path("roles").path("realm").findValuesAsText("name"))
        .containsExactly("customer");
    assertThat(realm.path("clients").findValuesAsText("clientId")).contains("client-app");
  }

  private static java.io.InputStream resource(String path) {
    return E2eTemplateTest.class.getClassLoader().getResourceAsStream(path);
  }
}
