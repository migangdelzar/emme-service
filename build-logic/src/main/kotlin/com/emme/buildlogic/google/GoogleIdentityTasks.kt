package com.emme.buildlogic.google

import com.fasterxml.jackson.databind.ObjectMapper
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

class GoogleIdentityTasksPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.tasks.register<ConfigureCustomerOidcTask>("configureCustomerOidc") {
      group = "identity"
      description = "Configure Google OIDC customer login in the emme-customers Keycloak realm"
      credentialsPath.convention(
        project.providers
          .gradleProperty("googleCustomerOAuthJson")
          .orElse(project.providers.environmentVariable("GOOGLE_CUSTOMER_OAUTH_JSON")),
      )
    }
    project.tasks.register<ConfigureTenantOAuthTask>("configureTenantOAuth") {
      group = "identity"
      description = "Configure the salon/studio Google OAuth2 client and validate its backend callback"
      credentialsPath.convention(
        project.providers
          .gradleProperty("googleTenantOAuthJson")
          .orElse(project.providers.environmentVariable("GOOGLE_TENANT_OAUTH_JSON")),
      )
    }
  }
}

@DisableCachingByDefault(because = "Updates external Keycloak state")
abstract class ConfigureCustomerOidcTask : DefaultTask() {
  @get:Optional
  @get:Input
  abstract val credentialsPath: Property<String>

  @TaskAction
  fun configure() {
    val credentials =
      loadCredentials {
        it.redirectUris.any { uri -> uri.contains("/realms/emme-customers/broker/google/endpoint") }
      }
    val client =
      KeycloakClient(
        baseUrl = env("KEYCLOAK_URL", "http://localhost:18080"),
        adminRealm = env("KEYCLOAK_ADMIN_REALM", "master"),
        adminUsername = env("KEYCLOAK_ADMIN_USERNAME", "admin"),
        adminPassword =
          env(
            "KEYCLOAK_ADMIN_PASSWORD",
            System.getenv("EMME_E2E_KEYCLOAK_ADMIN_PASSWORD") ?: "e2e-admin-password",
          ),
      )
    client.configureGoogleProvider(
      realm = env("KEYCLOAK_CUSTOMER_REALM", "emme-customers"),
      credentials = credentials,
    )
    logger.lifecycle("Google OIDC customer provider configured in emme-customers: ${credentials.clientId}")
  }

  private fun loadCredentials(predicate: (GoogleOAuthCredentials) -> Boolean): GoogleOAuthCredentials =
    GoogleOAuthCredentialsLoader
      .load(
        GoogleOAuthCredentialsFileResolver.resolve(
          credentialsPath.orNull,
          File(System.getProperty("user.home")),
          predicate,
        ),
      ).also { credentials ->
        require(predicate(credentials)) {
          "Customer Google OAuth JSON must contain the emme-customers Keycloak broker callback"
        }
      }
}

@DisableCachingByDefault(because = "Validates external Google OAuth configuration")
abstract class ConfigureTenantOAuthTask : DefaultTask() {
  @get:Optional
  @get:Input
  abstract val credentialsPath: Property<String>

  @TaskAction
  fun configure() {
    val credentials =
      GoogleOAuthCredentialsLoader.load(
        GoogleOAuthCredentialsFileResolver.resolve(
          credentialsPath.orNull,
          File(System.getProperty("user.home")),
        ) { candidate ->
          candidate.redirectUris.none { uri -> uri.contains("/realms/emme-customers/broker/google/endpoint") }
        },
      )
    val redirectUri = env("GOOGLE_OAUTH_REDIRECT_URI", "http://localhost:8080/api/google/oauth/callback")
    require(redirectUri in credentials.redirectUris) {
      "Tenant Google OAuth client is missing required redirect URI: $redirectUri. " +
        "Add that exact URI to the Google Web OAuth client and rerun configureTenantOAuth."
    }
    logger.lifecycle("Tenant Google OAuth2 client configured for runtime binding: ${credentials.clientId}")
    logger.lifecycle("Runtime mapping: GOOGLE_OAUTH_CLIENT_ID / GOOGLE_OAUTH_CLIENT_SECRET")
    logger.lifecycle("Redirect URI: $redirectUri")
    logger.lifecycle("Credentials were not printed or persisted; inject them through the runtime secret provider.")
  }
}

private class KeycloakClient(
  private val baseUrl: String,
  private val adminRealm: String,
  private val adminUsername: String,
  private val adminPassword: String,
  private val http: HttpClient = HttpClient.newHttpClient(),
  private val mapper: ObjectMapper = ObjectMapper(),
) {
  private companion object {
    const val HTTP_SUCCESS_MIN = 200
    const val HTTP_SUCCESS_MAX = 299
    const val HTTP_NOT_FOUND = 404
  }

  fun configureGoogleProvider(
    realm: String,
    credentials: GoogleOAuthCredentials,
  ) {
    val token =
      postForm(
        "/realms/$adminRealm/protocol/openid-connect/token",
        mapOf(
          "grant_type" to "password",
          "client_id" to "admin-cli",
          "username" to adminUsername,
          "password" to adminPassword,
        ),
      ).path("access_token").asText("")
    require(token.isNotBlank()) { "Keycloak did not return an administrative access token" }

    val providerPath = "/admin/realms/$realm/identity-provider/instances/google"
    val payload =
      mapper
        .createObjectNode()
        .apply {
          put("alias", "google")
          put("displayName", "Google")
          put("providerId", "google")
          put("enabled", true)
          put("trustEmail", true)
          put("storeToken", false)
          put("firstBrokerLoginFlowAlias", "first broker login")
          putObject("config").apply {
            put("clientId", credentials.clientId)
            put("clientSecret", credentials.clientSecret)
            put("defaultScope", "openid profile email")
          }
        }.toString()

    val existing = request("GET", providerPath, token, null)
    when (existing.statusCode()) {
      HTTP_SUCCESS_MIN -> {
        request("PUT", providerPath, token, payload).requireSuccess("update Google provider")
      }

      HTTP_NOT_FOUND -> {
        request("POST", "/admin/realms/$realm/identity-provider/instances", token, payload)
          .requireSuccess("create Google provider")
      }

      else -> {
        error("Keycloak provider lookup failed with HTTP ${existing.statusCode()}")
      }
    }
  }

  private fun postForm(
    path: String,
    values: Map<String, String>,
  ): com.fasterxml.jackson.databind.JsonNode {
    val body =
      values.entries.joinToString("&") { (key, value) ->
        "${encode(key)}=${encode(value)}"
      }
    val response = request("POST", path, null, body, "application/x-www-form-urlencoded")
    response.requireSuccess("request Keycloak admin token")
    return mapper.readTree(response.body())
  }

  private fun request(
    method: String,
    path: String,
    token: String?,
    body: String?,
    contentType: String = "application/json",
  ): HttpResponse<String> {
    val builder =
      HttpRequest
        .newBuilder(URI.create(baseUrl.trimEnd('/') + path))
        .header("Accept", "application/json")
    token?.let { builder.header("Authorization", "Bearer $it") }
    body?.let {
      builder.header("Content-Type", contentType)
      builder.method(method, HttpRequest.BodyPublishers.ofString(it, StandardCharsets.UTF_8))
    } ?: builder.method(method, HttpRequest.BodyPublishers.noBody())
    return http.send(builder.build(), HttpResponse.BodyHandlers.ofString())
  }

  private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

  private fun HttpResponse<String>.requireSuccess(operation: String) {
    require(statusCode() in HTTP_SUCCESS_MIN..HTTP_SUCCESS_MAX) {
      "Failed to $operation: HTTP ${statusCode()}"
    }
  }
}

private fun env(
  name: String,
  default: String,
): String = System.getenv(name)?.takeIf { it.isNotBlank() } ?: default
