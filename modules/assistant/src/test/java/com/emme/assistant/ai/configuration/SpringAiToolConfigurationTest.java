package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.assistant.ai.application.port.out.AiToolIdempotencyStore;
import com.emme.assistant.ai.application.tool.AiToolDefinition;
import com.emme.assistant.ai.application.tool.AiToolGateway;
import com.emme.assistant.ai.application.tool.AuthorizedAiToolGateway;
import com.emme.assistant.ai.application.tool.NoopAiToolIdempotencyStore;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class SpringAiToolConfigurationTest {

  @Test
  void wiresTheControlledGatewayFromTypedToolDefinitions() {
    AiToolDefinition definition =
        new AiToolDefinition(
            "getSalonServices",
            "List active salon services",
            Set.of("client"),
            com.emme.assistant.ai.application.tool.AiToolRisk.READ_ONLY,
            false,
            false,
            (context, arguments) -> "services");

    AiToolGateway gateway = new SpringAiToolConfiguration().aiToolGateway(List.of(definition));

    assertThat(gateway).isInstanceOf(AuthorizedAiToolGateway.class);
  }

  @Test
  void wiresTheTraceRecorderIntoTheControlledGateway() {
    AiToolDefinition definition =
        new AiToolDefinition(
            "getSalonServices",
            "List active salon services",
            Set.of("client"),
            com.emme.assistant.ai.application.tool.AiToolRisk.READ_ONLY,
            false,
            false,
            (context, arguments) -> "services");

    AiToolGateway gateway =
        new SpringAiToolConfiguration()
            .aiToolGateway(List.of(definition), org.mockito.Mockito.mock(AiTraceRecorder.class));

    assertThat(gateway).isInstanceOf(AuthorizedAiToolGateway.class);
  }

  @Test
  void wiresTheDurableIdempotencyStoreIntoTheControlledGateway() {
    AiToolDefinition definition =
        new AiToolDefinition(
            "createAppointment",
            "Create an appointment",
            Set.of("client"),
            com.emme.assistant.ai.application.tool.AiToolRisk.MUTATION,
            true,
            false,
            (context, arguments) -> "created");

    AiToolGateway gateway =
        new SpringAiToolConfiguration()
            .aiToolGateway(
                List.of(definition),
                org.mockito.Mockito.mock(AiTraceRecorder.class),
                org.mockito.Mockito.mock(AiToolIdempotencyStore.class));

    assertThat(gateway).isInstanceOf(AuthorizedAiToolGateway.class);
  }

  @Test
  void selectsTheJdbcIdempotencyStoreWhenTheTenantJdbcBoundaryExists() {
    AiToolIdempotencyStore store =
        new SpringAiToolConfiguration()
            .aiToolIdempotencyStore(
                Optional.of(org.mockito.Mockito.mock(JdbcClient.class)), new ObjectMapper());

    assertThat(store)
        .isInstanceOf(
            com.emme.assistant.ai.adapter.out.persistence.JdbcAiToolIdempotencyStore.class);
  }

  @Test
  void selectsTheNoopIdempotencyStoreWhenJdbcIsUnavailable() {
    AiToolIdempotencyStore store =
        new SpringAiToolConfiguration()
            .aiToolIdempotencyStore(Optional.empty(), new ObjectMapper());

    assertThat(store).isSameAs(NoopAiToolIdempotencyStore.INSTANCE);
  }
}
