/**
 * Reusable managed JDBC connection execution primitives for outbound adapters.
 *
 * <p>Use {@code JdbcConnectionExecutor.withConnection(function)} for result-producing work and
 * {@code JdbcConnectionExecutor.consumeWithConnection(consumer)} for side effects. The callback
 * receives a Spring-managed connection; callers must not acquire, close, or cache it.
 */
package com.emme.shared.persistence.jdbc;
