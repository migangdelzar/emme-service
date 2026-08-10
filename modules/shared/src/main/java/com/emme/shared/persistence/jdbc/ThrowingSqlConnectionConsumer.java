package com.emme.shared.persistence.jdbc;

import java.sql.Connection;

/**
 * Performs side-effecting work with a Spring-managed JDBC connection and may raise a checked
 * failure.
 *
 * @param <E> the checked failure type declared by the callback
 */
@FunctionalInterface
public interface ThrowingSqlConnectionConsumer<E extends Throwable> {

  void accept(Connection connection) throws E;
}
