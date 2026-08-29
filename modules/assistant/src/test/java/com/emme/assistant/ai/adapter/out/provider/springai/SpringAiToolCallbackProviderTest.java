package com.emme.assistant.ai.adapter.out.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.tool.AiToolDefinition;
import com.emme.assistant.ai.application.tool.AiToolExecutionContext;
import com.emme.assistant.ai.application.tool.AiToolGateway;
import com.emme.assistant.ai.application.tool.AiToolRisk;
import com.emme.assistant.ai.application.tool.AuthorizedAiToolGateway;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

class SpringAiToolCallbackProviderTest {

  @Test
  void exposesClosedWorldSchemasAndDelegatesCallsThroughTheAuthorizedGateway() {
    UUID tenantId = UUID.randomUUID();
    AiExecutionContext context = context(tenantId);
    var handler = mock(com.emme.assistant.ai.application.tool.AiToolHandler.class);
    when(handler.execute(
            new AiToolExecutionContext(
                context.tenantId(),
                context.principalId(),
                context.roles(),
                context.conversationId(),
                context.workflowId(),
                context.traceId(),
                context.idempotencyKey()),
            Map.of("serviceId", "service", "date", "2026-08-29")))
        .thenReturn("slots");
    AiToolGateway gateway =
        new AuthorizedAiToolGateway(
            Set.of(
                new AiToolDefinition(
                    "findAvailability",
                    "Find available appointment slots",
                    Set.of("client"),
                    AiToolRisk.READ_ONLY,
                    false,
                    false,
                    handler,
                    Set.of("serviceId", "date"),
                    Set.of("serviceId", "date"))));
    SpringAiToolCallbackProvider provider =
        new SpringAiToolCallbackProvider(gateway, new ObjectMapper());

    ToolCallback[] callbacks = AiExecutionContextScope.call(context, provider::getToolCallbacks);

    assertThat(callbacks).hasSize(1);
    assertThat(callbacks[0].getToolDefinition().name()).isEqualTo("findAvailability");
    assertThat(callbacks[0].getToolDefinition().inputSchema())
        .contains("\"additionalProperties\":false")
        .contains("\"serviceId\"")
        .contains("\"date\"")
        .contains("\"required\"");
    assertThat(
            AiExecutionContextScope.call(
                context,
                () -> callbacks[0].call("{\"serviceId\":\"service\",\"date\":\"2026-08-29\"}")))
        .isEqualTo("\"slots\"");
    verify(handler)
        .execute(
            new AiToolExecutionContext(
                context.tenantId(),
                context.principalId(),
                context.roles(),
                context.conversationId(),
                context.workflowId(),
                context.traceId(),
                context.idempotencyKey()),
            Map.of("serviceId", "service", "date", "2026-08-29"));
  }

  private static AiExecutionContext context(UUID tenantId) {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        tenantId, UUID.randomUUID(), Set.of("client"), id, id, "trace-" + id, "idem-" + id);
  }
}
