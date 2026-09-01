package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.adapter.out.persistence.JdbcAiTraceRecorder;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.assistant.ai.application.trace.AiTraceRedactor;
import com.emme.assistant.ai.application.trace.NoopAiTraceRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Selects durable PostgreSQL traces when JDBC is available, otherwise a safe no-op. */
@Configuration(proxyBeanMethods = false)
public class SpringAiTraceConfiguration {

  @Bean
  @ConditionalOnMissingBean
  AiTraceRedactor aiTraceRedactor() {
    return new AiTraceRedactor();
  }

  @Bean
  @ConditionalOnMissingBean(AiTraceRecorder.class)
  AiTraceRecorder aiTraceRecorder(
      Optional<JdbcClient> jdbc, AiTraceRedactor redactor, ObjectMapper objectMapper) {
    return jdbc.<AiTraceRecorder>map(
            client -> new JdbcAiTraceRecorder(client, redactor, objectMapper))
        .orElse(NoopAiTraceRecorder.INSTANCE);
  }

  AiTraceRecorder aiTraceRecorder(Optional<JdbcClient> jdbc, AiTraceRedactor redactor) {
    return aiTraceRecorder(jdbc, redactor, new ObjectMapper());
  }
}
