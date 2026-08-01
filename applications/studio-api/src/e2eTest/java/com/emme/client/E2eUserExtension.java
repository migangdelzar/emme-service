package com.emme.client;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit 5 extension that acquires an E2E test user from the pool, creates a {@link UserSession},
 * and injects it into test method parameters.
 *
 * <p>Resolution order: {@code @WithUser} on method → class level. Default if absent:
 * PLATFORM_ADMIN, any tenant, PER_METHOD.
 */
public class E2eUserExtension
    implements BeforeEachCallback,
        AfterEachCallback,
        BeforeAllCallback,
        AfterAllCallback,
        ParameterResolver {

  private static final E2eUserPool POOL = E2eUserPool.INSTANCE;
  private static final String USER_KEY = "e2e.user";
  private static final String SESSION_KEY = "e2e.session";

  // ── Acquire ──

  @Override
  public void beforeAll(ExtensionContext ctx) {
    var ann = resolve(ctx);
    if (ann.lifecycle() == WithUser.Lifecycle.PER_CLASS) {
      acquire(ctx, ann);
    }
  }

  @Override
  public void beforeEach(ExtensionContext ctx) {
    var ann = resolve(ctx);
    if (ann.lifecycle() == WithUser.Lifecycle.PER_METHOD) {
      acquire(ctx, ann);
    }
  }

  private void acquire(ExtensionContext ctx, WithUser ann) {
    var user =
        ann.tenant().isEmpty() ? POOL.acquire(ann.role()) : POOL.acquire(ann.role(), ann.tenant());
    var session = new UserSession(E2eTest.baseUrl(), user);
    getStore(ctx).put(USER_KEY, user);
    getStore(ctx).put(SESSION_KEY, session);
  }

  // ── Release ──

  @Override
  public void afterEach(ExtensionContext ctx) {
    releaseIf(ctx, WithUser.Lifecycle.PER_METHOD);
  }

  @Override
  public void afterAll(ExtensionContext ctx) {
    releaseIf(ctx, WithUser.Lifecycle.PER_CLASS);
  }

  private void releaseIf(ExtensionContext ctx, WithUser.Lifecycle scope) {
    var ann = resolve(ctx);
    if (ann.lifecycle() != scope) return;
    var session = getStore(ctx).get(SESSION_KEY, UserSession.class);
    if (session != null) session.close();
    var user = getStore(ctx).get(USER_KEY, E2eUserPool.TestUser.class);
    if (user != null) POOL.release(user.userId());
  }

  // ── Injection ──

  @Override
  public boolean supportsParameter(ParameterContext pc, ExtensionContext ctx) {
    return pc.getParameter().getType() == UserSession.class;
  }

  @Override
  public Object resolveParameter(ParameterContext pc, ExtensionContext ctx) {
    return getStore(ctx).get(SESSION_KEY, UserSession.class);
  }

  // ── Helpers ──

  private static WithUser resolve(ExtensionContext ctx) {
    // Method-level wins
    var method = ctx.getTestMethod();
    if (method.isPresent() && method.get().isAnnotationPresent(WithUser.class)) {
      return method.get().getAnnotation(WithUser.class);
    }
    // Fall back to class-level
    var testClass = ctx.getTestClass();
    if (testClass.isPresent() && testClass.get().isAnnotationPresent(WithUser.class)) {
      return testClass.get().getAnnotation(WithUser.class);
    }
    throw new IllegalStateException("@WithUser required on method or class for E2eUserExtension");
  }

  private static ExtensionContext.Store getStore(ExtensionContext ctx) {
    return ctx.getStore(
        ExtensionContext.Namespace.create(E2eUserExtension.class, ctx.getUniqueId()));
  }
}
