package com.emme.client;

import com.emme.client.E2eUserPool.TestUser;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit 5 extension that provisions and injects one or more E2E user sessions.
 *
 * <p>Method-level {@link WithUser} declarations override class-level declarations. The extension
 * owns user acquisition, token selection, and cleanup, so tests only express the identity they
 * need:
 *
 * <pre>{@code
 * @ExtendWith(E2eUserExtension.class)
 * @WithUser(tokenEnvironmentVariable = "E2E_OWNER_TOKEN")
 * @WithUser(role = Roles.TENANT_STAFF, tokenEnvironmentVariable = "E2E_STAFF_TOKEN")
 * class TenantBoundaryTest {
 *   @Test
 *   void staffCannotAccessOwnerData(E2eUsers users) {
 *     users.first().tenants().list();
 *     users.get(1).customers().list();
 *   }
 * }
 * }</pre>
 */
public final class E2eUserExtension
    implements BeforeAllCallback,
        AfterAllCallback,
        BeforeEachCallback,
        AfterEachCallback,
        ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(E2eUserExtension.class);
  private static final String USERS_KEY = "users";

  @Override
  public void beforeAll(ExtensionContext context) {
    var users = classUsers(context);
    if (!users.isEmpty() && lifecycle(users) == WithUser.Lifecycle.PER_CLASS) {
      acquire(context, users);
    }
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    var users = resolveUsers(context);
    if (lifecycle(users) == WithUser.Lifecycle.PER_METHOD) {
      acquire(context, users);
    }
  }

  @Override
  public void afterEach(ExtensionContext context) {
    var users = resolveUsers(context);
    if (lifecycle(users) == WithUser.Lifecycle.PER_METHOD) {
      release(context);
    }
  }

  @Override
  public void afterAll(ExtensionContext context) {
    var users = classUsers(context);
    if (!users.isEmpty() && lifecycle(users) == WithUser.Lifecycle.PER_CLASS) {
      release(context);
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
    var parameterType = parameter.getParameter().getType();
    if (parameterType == UserSession.class
        || parameterType == E2eUsers.class
        || parameterType == TestUser.class) {
      return true;
    }
    return parameterType == List.class
        && isUserSessionList(parameter.getParameter().getParameterizedType());
  }

  @Override
  public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
    var sessions = sessions(context);
    var parameterType = parameter.getParameter().getType();
    if (parameterType == E2eUsers.class) {
      return new E2eUsers(sessions);
    }
    if (parameterType == UserSession.class) {
      return sessions.getFirst();
    }
    if (parameterType == TestUser.class) {
      return users(context).getFirst();
    }
    return sessions;
  }

  private static void acquire(ExtensionContext context, List<WithUser> configurations) {
    var sessions = new ArrayList<UserSession>(configurations.size());
    var acquiredUsers = new ArrayList<TestUser>(configurations.size());
    try {
      for (int index = 0; index < configurations.size(); index++) {
        var configuration = configurations.get(index);
        if (!configuration.authenticated()) {
          sessions.add(new UserSession(E2eTest.baseUrl(), null, "", false));
          continue;
        }
        var user = E2eUserPool.INSTANCE.acquire(configuration.role(), configuration.tenant());
        acquiredUsers.add(user);
        sessions.add(
            new UserSession(E2eTest.baseUrl(), user, resolveToken(configuration, index), true));
      }
      store(context).put(USERS_KEY, new AcquiredUsers(acquiredUsers, sessions));
    } catch (RuntimeException exception) {
      sessions.forEach(UserSession::close);
      acquiredUsers.forEach(user -> E2eUserPool.INSTANCE.release(user.userId()));
      throw exception;
    }
  }

  private static void release(ExtensionContext context) {
    var acquired = storeForLifecycle(context).remove(USERS_KEY, AcquiredUsers.class);
    if (acquired == null) {
      return;
    }
    acquired.sessions().forEach(UserSession::close);
    acquired.users().forEach(user -> E2eUserPool.INSTANCE.release(user.userId()));
  }

  private static List<WithUser> resolveUsers(ExtensionContext context) {
    var methodConfigurations = methodUsers(context);
    if (!methodConfigurations.isEmpty()) {
      if (methodConfigurations.stream()
          .anyMatch(configuration -> configuration.lifecycle() == WithUser.Lifecycle.PER_CLASS)) {
        throw new IllegalStateException(
            "@WithUser(lifecycle = PER_CLASS) must be declared on the test class");
      }
      return methodConfigurations;
    }
    var classConfigurations = classUsers(context);
    if (classConfigurations.isEmpty()) {
      throw new IllegalStateException(
          "E2eUserExtension requires @WithUser on the test method or class");
    }
    return classConfigurations;
  }

  private static List<WithUser> methodUsers(ExtensionContext context) {
    return context
        .getTestMethod()
        .map(method -> Arrays.asList(method.getAnnotationsByType(WithUser.class)))
        .orElseGet(List::of);
  }

  private static List<WithUser> classUsers(ExtensionContext context) {
    return context
        .getTestClass()
        .map(testClass -> Arrays.asList(testClass.getAnnotationsByType(WithUser.class)))
        .orElseGet(List::of);
  }

  private static WithUser.Lifecycle lifecycle(List<WithUser> configurations) {
    var lifecycle = configurations.getFirst().lifecycle();
    if (configurations.stream().anyMatch(configuration -> configuration.lifecycle() != lifecycle)) {
      throw new IllegalStateException("All @WithUser declarations must use the same lifecycle");
    }
    return lifecycle;
  }

  private static String resolveToken(WithUser configuration, int index) {
    if (!configuration.tokenEnvironmentVariable().isBlank()) {
      return propertyOrEnvironment(configuration.tokenEnvironmentVariable());
    }
    var indexed = propertyOrEnvironment("E2E_ACCESS_TOKEN_" + (index + 1));
    return indexed.isBlank() ? propertyOrEnvironment("E2E_ACCESS_TOKEN") : indexed;
  }

  private static String propertyOrEnvironment(String name) {
    return System.getProperty(name, System.getenv().getOrDefault(name, ""));
  }

  private static List<UserSession> sessions(ExtensionContext context) {
    var acquired = storeForLifecycle(context).get(USERS_KEY, AcquiredUsers.class);
    if (acquired == null) {
      throw new IllegalStateException("E2E users were not acquired before parameter resolution");
    }
    return acquired.sessions();
  }

  private static List<TestUser> users(ExtensionContext context) {
    var acquired = storeForLifecycle(context).get(USERS_KEY, AcquiredUsers.class);
    if (acquired == null) {
      throw new IllegalStateException("E2E users were not acquired before parameter resolution");
    }
    return acquired.users();
  }

  private static ExtensionContext.Store store(ExtensionContext context) {
    return context.getStore(NAMESPACE);
  }

  private static ExtensionContext.Store storeForLifecycle(ExtensionContext context) {
    if (lifecycle(resolveUsers(context)) == WithUser.Lifecycle.PER_METHOD) {
      return store(context);
    }
    return classStore(context);
  }

  private static ExtensionContext.Store classStore(ExtensionContext context) {
    var current = context;
    while (true) {
      if (current.getTestClass().isPresent() && current.getTestMethod().isEmpty()) {
        return store(current);
      }
      current = current.getParent().orElse(null);
      if (current == null) {
        return store(context);
      }
    }
  }

  private static boolean isUserSessionList(Type type) {
    return type instanceof ParameterizedType parameterized
        && parameterized.getActualTypeArguments().length == 1
        && parameterized.getActualTypeArguments()[0] == UserSession.class;
  }

  private record AcquiredUsers(List<TestUser> users, List<UserSession> sessions) {}
}
