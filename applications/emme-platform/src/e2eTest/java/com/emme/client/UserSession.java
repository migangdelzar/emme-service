package com.emme.client;

import com.emme.client.E2eUserPool.TestUser;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Per-user HTTP session. Each test user gets their own OkHttpClient with optional auth token
 * injection.
 *
 * <p>Created by {@link E2eUserExtension} for annotation-driven tests or by {@link E2eTest} for
 * compatibility with existing flow helpers.
 */
public final class UserSession implements AutoCloseable {

  private final URI baseUrl;
  private final TestUser user;
  private final String accessToken;
  private final OkHttpClient httpClient;

  /** Authenticated session. */
  UserSession(URI baseUrl, TestUser user) {
    this(baseUrl, user, resolveAccessToken(), true);
  }

  /** Session with optional auth. {@code authenticated=false} skips token injection. */
  UserSession(URI baseUrl, TestUser user, boolean authenticated) {
    this(baseUrl, user, resolveAccessToken(), authenticated);
  }

  /** Session with an explicit token, which is required for multiple-user scenarios. */
  UserSession(URI baseUrl, TestUser user, String accessToken) {
    this(baseUrl, user, accessToken, true);
  }

  /** Session with explicit token and authentication mode. */
  UserSession(URI baseUrl, TestUser user, String accessToken, boolean authenticated) {
    this.baseUrl = baseUrl;
    this.user = user != null ? user : E2eUserPool.newTestUser(-1);
    this.accessToken = accessToken == null ? "" : accessToken;
    this.httpClient = buildClient(authenticated);
  }

  private OkHttpClient buildClient(boolean authenticated) {
    var logging = new HttpLoggingInterceptor();
    logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

    var builder =
        new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(30))
            .retryOnConnectionFailure(true)
            .addInterceptor(
                chain ->
                    chain.proceed(
                        chain.request().newBuilder().header("API-Version", "1.0").build()))
            .addInterceptor(logging);

    if (authenticated) {
      builder.addInterceptor(
          chain -> {
            if (!accessToken.isEmpty()) {
              var req =
                  chain
                      .request()
                      .newBuilder()
                      .header("Authorization", "Bearer " + accessToken)
                      .build();
              return chain.proceed(req);
            }
            return chain.proceed(chain.request());
          });
    }

    return builder.build();
  }

  /** The test user (dummy for unauthenticated sessions). */
  public TestUser user() {
    return user;
  }

  /** Returns the token used by this session, without exposing it in logs. */
  public String accessToken() {
    return accessToken;
  }

  /** Returns the tenant claim from this session's token, falling back to the fixture metadata. */
  public String tenantId() {
    var tokenTenantId = jwtClaim("tenant_id");
    if (!tokenTenantId.isBlank()) {
      return tokenTenantId;
    }
    return System.getProperty(
        "E2E_TENANT_ID", System.getenv().getOrDefault("E2E_TENANT_ID", user.tenantId()));
  }

  /** Returns the raw OkHttpClient (advanced use). */
  public OkHttpClient client() {
    return httpClient;
  }

  // ── HTTP methods ──

  /** GET → returns response body as String. Asserts 2xx. */
  public String get(String path) {
    return execute(new Request.Builder().url(resolve(path)).get().build(), 200);
  }

  /** GET → accept specific status, return body. */
  public String get(String path, int expectedStatus) {
    return execute(new Request.Builder().url(resolve(path)).get().build(), expectedStatus);
  }

  /** POST → assert 201 Created with Location header. */
  public String post(String path, String jsonBody) {
    var body = RequestBody.create(jsonBody, MediaType.get("application/json"));
    return execute(new Request.Builder().url(resolve(path)).post(body).build(), 201);
  }

  /** POST → accept specific status, return body. */
  public String post(String path, String jsonBody, int expectedStatus) {
    var body = RequestBody.create(jsonBody, MediaType.get("application/json"));
    return execute(new Request.Builder().url(resolve(path)).post(body).build(), expectedStatus);
  }

  /** PUT → assert 200. */
  public String put(String path, String jsonBody) {
    var body = RequestBody.create(jsonBody, MediaType.get("application/json"));
    return execute(new Request.Builder().url(resolve(path)).put(body).build(), 200);
  }

  /** PATCH → assert 200. */
  public String patch(String path, String jsonBody) {
    var body = RequestBody.create(jsonBody, MediaType.get("application/json"));
    return execute(new Request.Builder().url(resolve(path)).patch(body).build(), 200);
  }

  /** DELETE → assert 2xx. */
  public void delete(String path) {
    execute(new Request.Builder().url(resolve(path)).delete().build(), 200);
  }

  /** GET raw Response (caller must close). */
  public Response rawGet(String path) throws IOException {
    return httpClient.newCall(new Request.Builder().url(resolve(path)).get().build()).execute();
  }

  // ── Internal ──

  private String resolve(String path) {
    return baseUrl.resolve(path).toString();
  }

  private String execute(Request request, int expectedStatus) {
    try (var resp = httpClient.newCall(request).execute()) {
      var body = resp.body() != null ? resp.body().string() : "";
      if (resp.code() != expectedStatus) {
        throw new AssertionError(
            "Expected "
                + expectedStatus
                + " but got "
                + resp.code()
                + " for "
                + request.method()
                + " "
                + request.url()
                + "\nBody: "
                + body);
      }
      // Verify Location for 201
      if (expectedStatus == 201) {
        var loc = resp.header("Location");
        if (loc == null || loc.isEmpty()) {
          throw new AssertionError("201 Created must include Location header for " + request.url());
        }
      }
      return body;
    } catch (IOException e) {
      throw new RuntimeException("HTTP request failed: " + request.url(), e);
    }
  }

  // ── Module APIs ──

  public com.emme.client.crud.TenantCrud tenants() {
    return new com.emme.client.crud.TenantCrud(this);
  }

  public com.emme.client.crud.CustomerCrud customers() {
    return new com.emme.client.crud.CustomerCrud(this);
  }

  public com.emme.client.crud.ServiceCrud services() {
    return new com.emme.client.crud.ServiceCrud(this);
  }

  public com.emme.client.crud.ArtistCrud artists() {
    return new com.emme.client.crud.ArtistCrud(this);
  }

  public com.emme.client.crud.AppointmentCrud appointments() {
    return new com.emme.client.crud.AppointmentCrud(this);
  }

  public com.emme.client.crud.PaymentCrud payments() {
    return new com.emme.client.crud.PaymentCrud(this);
  }

  public com.emme.client.crud.SubscriptionCrud subscriptions() {
    return new com.emme.client.crud.SubscriptionCrud(this);
  }

  public com.emme.client.crud.CatalogCrud catalog() {
    return new com.emme.client.crud.CatalogCrud(this);
  }

  public com.emme.client.crud.DocumentCrud documents() {
    return new com.emme.client.crud.DocumentCrud(this);
  }

  public com.emme.client.crud.NotificationCrud notifications() {
    return new com.emme.client.crud.NotificationCrud(this);
  }

  public com.emme.client.crud.IdentityCrud identity() {
    return new com.emme.client.crud.IdentityCrud(this);
  }

  public com.emme.client.crud.BusinessConfigCrud businessConfig() {
    return new com.emme.client.crud.BusinessConfigCrud(this);
  }

  public com.emme.client.crud.AiCrud ai() {
    return new com.emme.client.crud.AiCrud(this);
  }

  public SetupHelper setup() {
    return new SetupHelper(this);
  }

  @Override
  public void close() {
    httpClient.dispatcher().executorService().shutdown();
    httpClient.connectionPool().evictAll();
  }

  private static String resolveAccessToken() {
    return System.getProperty(
        "E2E_ACCESS_TOKEN", System.getenv().getOrDefault("E2E_ACCESS_TOKEN", ""));
  }

  private String jwtClaim(String name) {
    if (accessToken.isBlank()) {
      return "";
    }
    try {
      var parts = accessToken.split("\\.");
      if (parts.length < 2) {
        return "";
      }
      var payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
      return E2eJson.stringField(payload, name);
    } catch (IllegalArgumentException exception) {
      return "";
    }
  }
}
