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
    var audienceMapper = client.putArray("protocolMappers").addObject();
    audienceMapper.put("name", "emme-platform-audience");
    audienceMapper.put("protocol", "openid-connect");
    audienceMapper.put("protocolMapper", "oidc-audience-mapper");
    audienceMapper
        .putObject("config")
        .put("included.client.audience", "emme-salon-app")
        .put("id.token.claim", "false")
        .put("access.token.claim", "true");

    var roles = document.putObject("roles").putArray("realm");
    for (var role :
        new String[] {
          "business_owner", "business_manager", "front_desk", "nail_artist", "read_only"
        }) {
      roles.addObject().put("name", role);
    }

    var users = document.putArray("users");
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

    addProfileScope(scopes);
    addEmailScope(scopes);
    addRolesScope(scopes);

    ArrayNode defaults = document.putArray("defaultDefaultClientScopes");
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

  private static void addProfileScope(ArrayNode scopes) {
    var scope = addScope(scopes, "profile", true);
    var mappers = scope.putArray("protocolMappers");
    addUserAttributeMapper(mappers, "username", "username", "preferred_username");
    var fullName = mappers.addObject();
    fullName.put("name", "full name");
    fullName.put("protocol", "openid-connect");
    fullName.put("protocolMapper", "oidc-full-name-mapper");
    fullName
        .putObject("config")
        .put("id.token.claim", "true")
        .put("access.token.claim", "true")
        .put("userinfo.token.claim", "true")
        .put("introspection.token.claim", "true");
  }

  private static void addEmailScope(ArrayNode scopes) {
    var scope = addScope(scopes, "email", true);
    var mappers = scope.putArray("protocolMappers");
    addUserAttributeMapper(mappers, "email", "email", "email");
    var verified = mappers.addObject();
    verified.put("name", "email verified");
    verified.put("protocol", "openid-connect");
    verified.put("protocolMapper", "oidc-usermodel-property-mapper");
    verified
        .putObject("config")
        .put("user.attribute", "emailVerified")
        .put("claim.name", "email_verified")
        .put("jsonType.label", "boolean")
        .put("id.token.claim", "true")
        .put("access.token.claim", "true")
        .put("userinfo.token.claim", "true")
        .put("introspection.token.claim", "true");
  }

  private static void addRolesScope(ArrayNode scopes) {
    var scope = addScope(scopes, "roles", false);
    var mapper = scope.putArray("protocolMappers").addObject();
    mapper.put("name", "realm roles");
    mapper.put("protocol", "openid-connect");
    mapper.put("protocolMapper", "oidc-usermodel-realm-role-mapper");
    mapper
        .putObject("config")
        .put("user.attribute", "foo")
        .put("claim.name", "realm_access.roles")
        .put("jsonType.label", "String")
        .put("multivalued", "true")
        .put("id.token.claim", "true")
        .put("access.token.claim", "true")
        .put("userinfo.token.claim", "true")
        .put("introspection.token.claim", "true");
  }

  private static ObjectNode addScope(ArrayNode scopes, String name, boolean inTokenScope) {
    var scope = scopes.addObject();
    scope.put("name", name);
    scope.put("protocol", "openid-connect");
    scope
        .putObject("attributes")
        .put("include.in.token.scope", Boolean.toString(inTokenScope))
        .put("display.on.consent.screen", "false");
    return scope;
  }

  private static void addUserAttributeMapper(
      ArrayNode mappers, String name, String attribute, String claimName) {
    var mapper = mappers.addObject();
    mapper.put("name", name);
    mapper.put("protocol", "openid-connect");
    mapper.put("protocolMapper", "oidc-usermodel-attribute-mapper");
    mapper
        .putObject("config")
        .put("user.attribute", attribute)
        .put("claim.name", claimName)
        .put("jsonType.label", "String")
        .put("id.token.claim", "true")
        .put("access.token.claim", "true")
        .put("userinfo.token.claim", "true")
        .put("introspection.token.claim", "true");
  }
}
