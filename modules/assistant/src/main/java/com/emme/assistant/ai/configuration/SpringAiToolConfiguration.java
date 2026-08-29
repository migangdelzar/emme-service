package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.adapter.out.persistence.JdbcAiToolIdempotencyStore;
import com.emme.assistant.ai.adapter.out.provider.springai.SpringAiToolCallbackProvider;
import com.emme.assistant.ai.application.port.out.AiToolIdempotencyStore;
import com.emme.assistant.ai.application.tool.AiToolDefinition;
import com.emme.assistant.ai.application.tool.AiToolGateway;
import com.emme.assistant.ai.application.tool.AuthorizedAiToolGateway;
import com.emme.assistant.ai.application.tool.NoopAiToolIdempotencyStore;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.assistant.ai.application.trace.NoopAiTraceRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Wires typed tool definitions into the controlled in-process gateway. */
@Configuration(proxyBeanMethods = false)
public class SpringAiToolConfiguration {

  @Bean
  @ConditionalOnMissingBean(AiToolGateway.class)
  AiToolGateway aiToolGateway(
      List<AiToolDefinition> definitions,
      AiTraceRecorder traceRecorder,
      AiToolIdempotencyStore idempotencyStore) {
    return new AuthorizedAiToolGateway(definitions, traceRecorder, idempotencyStore);
  }

  AiToolGateway aiToolGateway(List<AiToolDefinition> definitions) {
    return new AuthorizedAiToolGateway(definitions, NoopAiTraceRecorder.INSTANCE);
  }

  AiToolGateway aiToolGateway(List<AiToolDefinition> definitions, AiTraceRecorder traceRecorder) {
    return new AuthorizedAiToolGateway(definitions, traceRecorder);
  }

  @Bean
  @ConditionalOnMissingBean(AiToolIdempotencyStore.class)
  AiToolIdempotencyStore aiToolIdempotencyStore(
      @Qualifier("aiTenantJdbcClient") Optional<JdbcClient> jdbc, ObjectMapper objectMapper) {
    return jdbc.<AiToolIdempotencyStore>map(
            client -> new JdbcAiToolIdempotencyStore(client, objectMapper))
        .orElse(NoopAiToolIdempotencyStore.INSTANCE);
  }

  @Bean
  @ConditionalOnMissingBean
  SpringAiToolCallbackProvider springAiToolCallbackProvider(
      AiToolGateway gateway, ObjectMapper objectMapper) {
    return new SpringAiToolCallbackProvider(gateway, objectMapper);
  }
}
