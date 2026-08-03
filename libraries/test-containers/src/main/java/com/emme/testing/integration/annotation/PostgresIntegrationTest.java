package com.emme.testing.integration.annotation;

import com.emme.testing.integration.container.PostgresContainerConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Composed annotation for PostgreSQL-backed integration tests.
 *
 * <p>Pairs with Spring slice annotations ({@code @DataJpaTest}, {@code @JdbcTest}) to test against
 * real PostgreSQL instead of H2.
 *
 * <p>{@link PostgresContainerConfiguration @ServiceConnection} automatically overrides datasource
 * properties — no manual {@code @AutoConfigureTestDatabase} needed in Spring Boot 3.1+.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @DataJpaTest
 * @PostgresIntegrationTest
 * class BookingRepositoryIntegrationTest { ... }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("integration-test")
@Import(PostgresContainerConfiguration.class)
public @interface PostgresIntegrationTest {}
