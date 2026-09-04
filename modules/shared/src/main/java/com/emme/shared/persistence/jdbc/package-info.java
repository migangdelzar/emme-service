/**
 * Reusable managed JDBC connection execution primitives for outbound adapters.
 *
 * <p>Use {@code BootstrapConnectionExecutor.withConnection(function)} for result-producing work and
 * {@code BootstrapConnectionExecutor.consumeWithConnection(consumer)} for side effects. The
 * callback receives a Spring-managed connection; callers must not acquire, close, or cache it.
 */
@org.springframework.modulith.NamedInterface("persistence-jdbc")
package com.emme.shared.persistence.jdbc;
