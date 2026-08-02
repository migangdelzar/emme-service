package com.emme.tenancy.adapter.out.client.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

@SuppressWarnings("unchecked")
class JdbcConnectionExecutorTest {

  private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
  private final Connection connection = mock(Connection.class);
  private final JdbcConnectionExecutor executor = new JdbcConnectionExecutor(jdbcTemplate);

  @Test
  void returnsTheFunctionResultThroughSpringManagedConnectionExecution() {
    when(jdbcTemplate.execute(any(ConnectionCallback.class)))
        .thenAnswer(
            invocation ->
                invocation.<ConnectionCallback<String>>getArgument(0).doInConnection(connection));

    String result = executor.withConnection((SqlConnectionFunction<String>) ignored -> "result");
    assertThat(result).isEqualTo("result");
  }

  @Test
  void executesConsumerThroughSpringManagedConnectionExecution() throws Exception {
    when(jdbcTemplate.execute(any(ConnectionCallback.class)))
        .thenAnswer(
            invocation ->
                invocation.<ConnectionCallback<Void>>getArgument(0).doInConnection(connection));

    executor.withConnection(Connection::commit);

    verify(connection).commit();
  }
}
