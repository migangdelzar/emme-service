package com.emme.shared.persistence.jdbc;

import java.sql.Connection;

/**
 * Produces a result from a Spring-managed JDBC connection and may raise a checked failure.
 *
 * @param <R> the result type
 * @param <E> the checked failure type declared by the callback
 */
@FunctionalInterface
public interface ThrowingSqlConnectionFunction<R, E extends Throwable> {

  R apply(Connection connection) throws E;
}
