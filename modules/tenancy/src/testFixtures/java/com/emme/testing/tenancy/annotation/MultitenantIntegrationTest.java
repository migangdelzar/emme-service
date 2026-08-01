package com.emme.testing.tenancy.annotation;

import com.emme.testing.integration.annotation.PostgresIntegrationTest;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full-stack tenant-aware integration test annotation.
 *
 * <p>Composes {@link SpringBootTest} with {@link PostgresIntegrationTest} to bootstrap a real
 * PostgreSQL container and a full Spring context.
 *
 * <p>Tenancy module tests call the provisioning use-case directly. All other modules use {@link
 * com.emme.testing.tenancy.provisioning.TenantTestProvisioner}.
 *
 * <p>Requires a discoverable {@code @SpringBootConfiguration} in the module under test.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@PostgresIntegrationTest
public @interface MultitenantIntegrationTest {}
