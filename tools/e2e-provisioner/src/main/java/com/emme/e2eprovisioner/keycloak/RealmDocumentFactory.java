package com.emme.e2eprovisioner.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Creates the typed Keycloak realm document used by the disposable E2E environment. */
public final class RealmDocumentFactory {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private RealmDocumentFactory() {}

  public static ObjectNode create(RealmConfiguration configuration) {
    var document = MAPPER.createObjectNode();
    document.put("realm", "emme");
    document.put("enabled", true);
    document.put("sslRequired", "none");

    var clients = document.putArray("clients");
    var client = clients.addObject();
    client.put("clientId", "emme-salon-app");
    client.put("enabled", true);
    client.put("publicClient", true);
    client.put("directAccessGrantsEnabled", true);
    client.put("standardFlowEnabled", true);
    client.putArray("redirectUris").add(configuration.webOrigin() + "/*");
    client.putArray("webOrigins").add(configuration.webOrigin());

    var roles = document.putObject("roles").putArray("realm");
    for (var role :
        new String[] {
          "business_owner", "business_manager", "front_desk", "nail_artist", "read_only"
        }) {
      roles.addObject().put("name", role);
    }

    var users = document.putArray("users");
    var user = users.addObject();
    user.put("username", configuration.username());
    user.put("email", configuration.username());
    user.put("emailVerified", true);
    user.put("enabled", true);
    user.put("firstName", "E2E");
    user.put("lastName", "Owner");
    user.putArray("credentials")
        .addObject()
        .put("type", "password")
        .put("value", configuration.password())
        .put("temporary", false);
    user.putArray("realmRoles").add("business_owner");
    var attributes = user.putObject("attributes");
    attributes.putArray("tenant_id").add(configuration.tenantId().toString());
    attributes.putArray("tenant_slug").add(configuration.tenantSlug());

    var scopes = document.putArray("clientScopes");
    var tenantScope = scopes.addObject();
    tenantScope.put("name", "tenant-context");
    tenantScope.put("protocol", "openid-connect");
    tenantScope
        .putObject("attributes")
        .put("include.in.token.scope", "true")
        .put("display.on.consent.screen", "false");
    var mappers = tenantScope.putArray("protocolMappers");
    addTenantMapper(mappers, "tenant-id-mapper", "tenant_id");
    addTenantMapper(mappers, "tenant-slug-mapper", "tenant_slug");

    ArrayNode defaults = document.putArray("defaultClientScopes");
    for (var scope :
        new String[] {"tenant-context", "web-origins", "acr", "profile", "roles", "email"}) {
      defaults.add(scope);
    }
    return document;
  }

  private static void addTenantMapper(ArrayNode mappers, String name, String attribute) {
    var mapper = mappers.addObject();
    mapper.put("name", name);
    mapper.put("protocol", "openid-connect");
    mapper.put("protocolMapper", "oidc-usermodel-attribute-mapper");
    mapper
        .putObject("config")
        .put("user.attribute", attribute)
        .put("claim.name", attribute)
        .put("id.token.claim", "true")
        .put("access.token.claim", "true")
        .put("userinfo.token.claim", "true")
        .put("jsonType.label", "String");
  }
}
