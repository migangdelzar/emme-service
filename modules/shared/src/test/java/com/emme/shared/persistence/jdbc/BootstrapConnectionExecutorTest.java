package com.emme.shared.persistence.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

@SuppressWarnings("unchecked")
class BootstrapConnectionExecutorTest {

  private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
  private final Connection connection = mock(Connection.class);
  private final BootstrapConnectionExecutor executor =
      new BootstrapConnectionExecutor(jdbcTemplate);

  @Test
  void returnsTheFunctionResultThroughSpringManagedConnectionExecution() {
    when(jdbcTemplate.execute(any(ConnectionCallback.class)))
        .thenAnswer(
            invocation ->
                invocation.<ConnectionCallback<String>>getArgument(0).doInConnection(connection));

    String result =
        executor.withConnection(
            (ThrowingSqlConnectionFunction<String, SQLException>) ignored -> "result");

    assertThat(result).isEqualTo("result");
  }

  @Test
  void executesConsumerThroughSpringManagedConnectionExecution() throws Throwable {
    when(jdbcTemplate.execute(any(ConnectionCallback.class)))
        .thenAnswer(
            invocation ->
                invocation.<ConnectionCallback<Void>>getArgument(0).doInConnection(connection));

    ThrowingSqlConnectionConsumer<SQLException> consumer = Connection::commit;
    executor.consumeWithConnection(consumer);

    verify(connection).commit();
  }

  @Test
  void wrapsCheckedThrowablesWithoutSwallowingTheOriginalCause() {
    when(jdbcTemplate.execute(any(ConnectionCallback.class)))
        .thenAnswer(
            invocation ->
                invocation.<ConnectionCallback<Void>>getArgument(0).doInConnection(connection));
    SQLException expected = new SQLException("provider failure");
    ThrowingSqlConnectionFunction<Void, SQLException> failingFunction =
        ignored -> {
          throw expected;
        };

    assertThatThrownBy(() -> executor.withConnection(failingFunction))
        .isInstanceOf(JdbcConnectionExecutionException.class)
        .hasCause(expected);
  }

  @Test
  void rethrowsFatalErrorsUnchanged() {
    when(jdbcTemplate.execute(any(ConnectionCallback.class)))
        .thenAnswer(
            invocation ->
                invocation.<ConnectionCallback<Void>>getArgument(0).doInConnection(connection));
    AssertionError expected = new AssertionError("fatal failure");

    assertThatThrownBy(
            () ->
                executor.withConnection(
                    (ThrowingSqlConnectionFunction<Void, AssertionError>)
                        ignored -> {
                          throw expected;
                        }))
        .isSameAs(expected);
  }

  @Test
  void restoresTheInterruptFlagWhenTheCallbackIsInterrupted() {
    when(jdbcTemplate.execute(any(ConnectionCallback.class)))
        .thenAnswer(
            invocation ->
                invocation.<ConnectionCallback<Void>>getArgument(0).doInConnection(connection));
    InterruptedException expected = new InterruptedException("interrupted");

    try {
      assertThatThrownBy(
              () ->
                  executor.withConnection(
                      (ThrowingSqlConnectionFunction<Void, InterruptedException>)
                          ignored -> {
                            throw expected;
                          }))
          .isInstanceOf(JdbcConnectionExecutionException.class)
          .hasCause(expected);

      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void rejectsNullCallbacksBeforeDelegatingToSpring() {
    assertThatThrownBy(
            () ->
                executor.withConnection(
                    (ThrowingSqlConnectionFunction<String, RuntimeException>) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("function must not be null");

    assertThatThrownBy(
            () ->
                executor.consumeWithConnection(
                    (ThrowingSqlConnectionConsumer<RuntimeException>) null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("consumer must not be null");

    verifyNoInteractions(jdbcTemplate);
  }
}
