package com.emme.tenancy.adapter.out.client.database;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Executes connection work through Spring's managed JDBC resource lifecycle. */
@Component
@SuppressWarnings("overloads")
final class JdbcConnectionExecutor {

  private final JdbcTemplate jdbcTemplate;

  JdbcConnectionExecutor(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  <T> T withConnection(SqlConnectionFunction<T> function) {
    return jdbcTemplate.execute(
        (ConnectionCallback<T>)
            connection -> {
              try {
                return function.apply(connection);
              } catch (java.sql.SQLException exception) {
                throw exception;
              } catch (Exception exception) {
                throw new IllegalStateException("JDBC connection operation failed", exception);
              }
            });
  }

  void withConnection(SqlConnectionConsumer consumer) {
    withConnection(
        connection -> {
          consumer.accept(connection);
          return null;
        });
  }
}
