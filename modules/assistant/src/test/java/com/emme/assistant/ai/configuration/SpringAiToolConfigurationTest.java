package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.assistant.ai.application.tool.AiToolDefinition;
import com.emme.assistant.ai.application.tool.AiToolGateway;
import com.emme.assistant.ai.application.tool.AuthorizedAiToolGateway;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

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
}
