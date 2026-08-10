package com.emme.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures one authenticated or unauthenticated E2E user for a test method or class. Method-level
 * declarations override class-level declarations.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Repeatable(WithUsers.class)
public @interface WithUser {

  /** Realm role used to select the provisioned test user. */
  String role() default Roles.PLATFORM_ADMIN;

  /** Tenant ID. Empty string selects any tenant for the role. */
  String tenant() default "";

  /** Optional environment variable or system property containing this user's bearer token. */
  String tokenEnvironmentVariable() default "";

  /** Whether the session sends a bearer token. */
  boolean authenticated() default true;

  /** When to acquire and release the user. */
  Lifecycle lifecycle() default Lifecycle.PER_METHOD;

  enum Lifecycle {
    /** Fresh user per test method for isolation. */
    PER_METHOD,
    /** One user for all methods in the test class. */
    PER_CLASS
  }
}
