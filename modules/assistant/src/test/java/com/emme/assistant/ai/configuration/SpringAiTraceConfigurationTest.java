package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.assistant.ai.adapter.out.persistence.JdbcAiTraceRecorder;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.assistant.ai.application.trace.AiTraceRedactor;
import com.emme.assistant.ai.application.trace.NoopAiTraceRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class SpringAiTraceConfigurationTest {

  @Test
  void usesNoopTracingWhenJdbcIsUnavailable() {
    AiTraceRecorder recorder =
        new SpringAiTraceConfiguration()
            .aiTraceRecorder(Optional.empty(), new AiTraceRedactor(), new ObjectMapper());

    assertThat(recorder).isSameAs(NoopAiTraceRecorder.INSTANCE);
  }

  @Test
  void selectsTheJdbcRecorderWhenJdbcIsAvailable() {
    AiTraceRecorder recorder =
        new SpringAiTraceConfiguration()
            .aiTraceRecorder(
                Optional.of(mock(JdbcClient.class)), new AiTraceRedactor(), new ObjectMapper());

    assertThat(recorder).isInstanceOf(JdbcAiTraceRecorder.class);
  }

  @Test
  void requiresTheSpringManagedObjectMapperForTraceRecorders() {
    assertThat(JdbcAiTraceRecorder.class.getConstructors())
        .extracting(constructor -> constructor.getParameterTypes())
        .containsExactly(
            new Class<?>[] {JdbcClient.class, AiTraceRedactor.class, ObjectMapper.class});
  }
}
