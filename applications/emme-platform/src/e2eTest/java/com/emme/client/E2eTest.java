package com.emme.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Functional entry point for E2E tests.
 *
 * <h3>Usage</h3>
 *
 * <pre>{@code
 * class TenantApiTest {
 *     &#64;Test void shouldCreate() {
 *         withSession(s -> {
 *             var body = s.tenants().create("acme", "Acme Corp");
 *             assertThat(body).contains("acme");
 *         });
 *     }
 *
 *     &#64;Test void shouldFullFlow() {
 *         withSession(s -> {
 *             s.setup().subscription(DEMO_TENANT);
 *             s.customers().create("Jane", "j@t.com", "555");
 *             s.services().create("Manicure", "MANI-01", 350, 30, "Manicura");
 *             assertThat(s.tenants().list()).startsWith("[");
 *         });
 *     }
 * }
 * }</pre>
 *
 * <p>{@code withSession} handles:
 *
 * <ul>
 *   <li>URL resolution from {@code EMME_E2E_BASE_URL}
 *   <li>User acquisition from pool
 *   <li>UserSession creation (per-user OkHttp client)
 *   <li>Auth token injection via {@code E2E_TENANT_OWNER_TOKEN}, falling back to {@code
 *       E2E_ACCESS_TOKEN}
 *   <li>Resource cleanup (session close + user release)
 * </ul>
 */
public final class E2eTest {

  private static final URI BASE_URL = resolveBaseUrl();

  private E2eTest() {}

  private static URI resolveBaseUrl() {
    var url =
        System.getProperty(
            "emme.e2e.base-url", System.getenv().getOrDefault("EMME_E2E_BASE_URL", ""));
    if (url.isEmpty())
      throw new IllegalStateException(
          "Set EMME_E2E_BASE_URL or emme.e2e.base-url to run E2E tests");
    return URI.create(url);
  }

  /**
   * Execute a test block with a fully set-up UserSession. User is acquired from pool, session
   * created with auth, and cleaned up after.
   */
  public static void withSession(Consumer<UserSession> block) {
    withResult(
        s -> {
          block.accept(s);
          return null;
        });
  }

  /**
   * Execute a test block and return a result. Use for multi-user scenarios where one test needs
   * data from another session.
   *
   * <pre>{@code
   * var tenantId = withResult(s -> s.tenants().create("acme", "Acme").id());
   * withSession(other -> {
   *     assertThatThrownBy(() -> other.tenants().getById(tenantId))
   *         .hasMessageContaining("403");
   * });
   * }</pre>
   */
  public static <T> T withResult(Function<UserSession, T> block) {
    return withResult(resolveTenantOwnerToken(), block);
  }

  /** Execute with the platform token for control-plane APIs such as tenant provisioning. */
  public static void withPlatformSession(Consumer<UserSession> block) {
    withResult(
        resolvePlatformToken(),
        s -> {
          block.accept(s);
          return null;
        });
  }

  /** Executes a scenario with the supplied identities and releases every session afterwards. */
  public static void withUsers(Consumer<E2eUsers> block, E2eUserSpec... specifications) {
    var sessions = new ArrayList<UserSession>(specifications.length);
    var acquiredUsers = new ArrayList<E2eUserPool.TestUser>(specifications.length);
    try {
      for (var specification : specifications) {
        if (!specification.authenticated()) {
          sessions.add(new UserSession(BASE_URL, null, "", false));
          continue;
        }
        var user = E2eUserPool.INSTANCE.acquire(specification.roles(), specification.tenantId());
        acquiredUsers.add(user);
        sessions.add(new UserSession(BASE_URL, user, resolveToken(specification), true));
      }
      block.accept(new E2eUsers(sessions));
    } finally {
      sessions.forEach(UserSession::close);
      acquiredUsers.forEach(user -> E2eUserPool.INSTANCE.release(user.userId()));
    }
  }

  /** Executes a scenario with an explicit collection of user specifications. */
  public static void withUsers(List<E2eUserSpec> specifications, Consumer<E2eUsers> block) {
    Objects.requireNonNull(specifications, "specifications must not be null");
    Objects.requireNonNull(block, "block must not be null");
    withUsers(block, specifications.toArray(E2eUserSpec[]::new));
  }

  /** Convenience overload for one identity. */
  public static void withUser(E2eUserSpec specification, Consumer<UserSession> block) {
    withUsers(users -> block.accept(users.first()), specification);
  }

  private static <T> T withResult(String accessToken, Function<UserSession, T> block) {
    var user = E2eUserPool.INSTANCE.acquire();
    var session = new UserSession(BASE_URL, user, accessToken);
    try {
      return block.apply(session);
    } finally {
      session.close();
      E2eUserPool.INSTANCE.release(user.userId());
    }
  }

  /** Execute with a specific role requirement. Useful when different tests need different roles. */
  public static void withSession(String[] roles, Consumer<UserSession> block) {
    withUser(E2eUserSpec.authenticated(roles), block);
  }

  /** Execute without auth token injection (for testing 401/403 responses). */
  public static void withUnauthenticated(Consumer<UserSession> block) {
    var session = new UserSession(BASE_URL, null, false);
    try {
      block.accept(session);
    } finally {
      session.close();
    }
  }

  /** Return the configured base URL. */
  public static URI baseUrl() {
    return BASE_URL;
  }

  private static String resolveTenantOwnerToken() {
    var ownerToken = propertyOrEnvironment("E2E_TENANT_OWNER_TOKEN");
    return ownerToken.isBlank() ? resolvePlatformToken() : ownerToken;
  }

  private static String resolvePlatformToken() {
    return propertyOrEnvironment("E2E_ACCESS_TOKEN");
  }

  private static String resolveToken(E2eUserSpec specification) {
    if (!specification.accessToken().isBlank()) {
      return specification.accessToken();
    }
    return specification.roles().contains(Roles.PLATFORM_ADMIN)
        ? resolvePlatformToken()
        : resolveTenantOwnerToken();
  }

  private static String propertyOrEnvironment(String name) {
    return System.getProperty(name, System.getenv().getOrDefault(name, ""));
  }
}
