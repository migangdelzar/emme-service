package com.emme.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures the test user for an E2E test method or class.
 * Resolved by {@link E2eUserExtension}: method-level overrides class-level.
 *
 * <pre>{@code
 * @WithUser(role = BUSINESS_OWNER, tenant = "demo-salon")
 * class CustomerApiTest extends E2eBaseTest {
 *     @Test void list(UserSession api) { api.customers().list(); }
 * }
 * }</pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface WithUser {

    /** Realm role for the test user. Default: platform_admin. */
    Role role() default Role.PLATFORM_ADMIN;

    /** Tenant ID (UUID). Empty string = any tenant for this role. */
    String tenant() default "";

    /** When to acquire and release the user. */
    Lifecycle lifecycle() default Lifecycle.PER_METHOD;

    enum Lifecycle {
        /** Fresh user per @Test — best isolation. */
        PER_METHOD,
        /** One user for all tests in the class — faster for read-only suites. */
        PER_CLASS
    }
}
