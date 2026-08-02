package com.emme.shared.persistence.jdbc;

import java.util.Objects;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Reusable higher-order boundary for connection-scoped JDBC work.
 *
 * <p>Spring owns acquisition, transaction participation, thread binding, and cleanup through {@link
 * JdbcTemplate#execute(ConnectionCallback)}. Callers must not acquire or close a connection
 * themselves.
 */
@Component
public final class JdbcConnectionExecutor {

  private final JdbcTemplate jdbcTemplate;

  public JdbcConnectionExecutor(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Executes side-effecting work with a managed connection.
   *
   * @param consumer callback receiving the managed connection
   * @param <E> checked failure type declared by the callback
   */
  public <E extends Throwable> void consumeWithConnection(
      ThrowingSqlConnectionConsumer<E> consumer) {
    Objects.requireNonNull(consumer, "consumer must not be null");
    withConnection(
        (ThrowingSqlConnectionFunction<Void, E>)
            connection -> {
              consumer.accept(connection);
              return null;
            });
  }

  /**
   * Executes result-producing work with a managed connection.
   *
   * @param function callback receiving the managed connection
   * @param <R> result type
   * @param <E> checked failure type declared by the callback
   * @return the callback result
   */
  public <R, E extends Throwable> R withConnection(ThrowingSqlConnectionFunction<R, E> function) {
    Objects.requireNonNull(function, "function must not be null");
    return jdbcTemplate.execute(
        (ConnectionCallback<R>)
            connection -> {
              try {
                return function.apply(connection);
              } catch (Error error) {
                throw error;
              } catch (Throwable throwable) {
                if (throwable instanceof InterruptedException) {
                  Thread.currentThread().interrupt();
                }
                if (throwable instanceof JdbcConnectionExecutionException exception) {
                  throw exception;
                }
                throw new JdbcConnectionExecutionException(throwable);
              }
            });
  }
}
