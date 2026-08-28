package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.assistant.ai.application.trace.AiTraceRedactor;
import com.emme.assistant.ai.application.trace.NoopAiTraceRecorder;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class SpringAiTraceConfigurationTest {

  @Test
  void usesNoopTracingWhenJdbcIsUnavailable() {
    AiTraceRecorder recorder =
        new SpringAiTraceConfiguration().aiTraceRecorder(Optional.empty(), new AiTraceRedactor());

    assertThat(recorder).isSameAs(NoopAiTraceRecorder.INSTANCE);
  }

  @Test
  void selectsTheJdbcRecorderWhenJdbcIsAvailable() {
    AiTraceRecorder recorder =
        new SpringAiTraceConfiguration()
            .aiTraceRecorder(Optional.of(mock(JdbcClient.class)), new AiTraceRedactor());

    assertThat(recorder)
        .isInstanceOf(com.emme.assistant.ai.adapter.out.persistence.JdbcAiTraceRecorder.class);
  }
}
