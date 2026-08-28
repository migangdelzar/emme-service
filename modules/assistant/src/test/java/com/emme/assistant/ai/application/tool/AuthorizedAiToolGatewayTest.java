package com.emme.assistant.ai.application.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthorizedAiToolGatewayTest {

  @Test
  void executesAnAuthorizedReadOnlyToolWithBackendContext() {
    AiToolHandler handler = mock(AiToolHandler.class);
    AiExecutionContext context = context(Set.of("client"));
    when(handler.execute(
            new AiToolExecutionContext(
                context.tenantId(),
                context.principalId(),
                context.roles(),
                context.conversationId(),
                context.workflowId(),
                context.traceId(),
                context.idempotencyKey()),
            Map.of("locale", "es-MX")))
        .thenReturn("services");
    AuthorizedAiToolGateway gateway =
        new AuthorizedAiToolGateway(
            Set.of(
                new AiToolDefinition(
                    "getSalonServices",
                    "List active salon services",
                    Set.of("client"),
                    AiToolRisk.READ_ONLY,
                    false,
                    false,
                    handler)));

    AiToolResult result =
        AiExecutionContextScope.call(
            context,
            () ->
                gateway.execute(
                    new AiToolInvocation(
                        "getSalonServices", Map.of("locale", "es-MX"), false, false)));

    assertThat(result).isEqualTo(new AiToolResult("getSalonServices", "services", true));
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
            Map.of("locale", "es-MX"));
  }

  @Test
  void rejectsAReadOnlyToolWhenTheBackendRoleIsNotAllowed() {
    AuthorizedAiToolGateway gateway =
        new AuthorizedAiToolGateway(
            Set.of(
                new AiToolDefinition(
                    "getSalonServices",
                    "List active salon services",
                    Set.of("tenant_staff"),
                    AiToolRisk.READ_ONLY,
                    false,
                    false,
                    (context, arguments) -> "services")));

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    context(Set.of("client")),
                    () ->
                        gateway.execute(
                            new AiToolInvocation("getSalonServices", Map.of(), false, false))))
        .isInstanceOf(AiToolExecutionRejectedException.class)
        .hasMessage("AI tool is not authorized: getSalonServices");
  }

  @Test
  void refusesAWriteToolWithoutTheRequiredUserConfirmation() {
    AuthorizedAiToolGateway gateway =
        new AuthorizedAiToolGateway(
            Set.of(
                new AiToolDefinition(
                    "createAppointment",
                    "Create an appointment",
                    Set.of("client"),
                    AiToolRisk.MUTATION,
                    true,
                    false,
                    (context, arguments) -> "created")));

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    context(Set.of("client")),
                    () ->
                        gateway.execute(
                            new AiToolInvocation("createAppointment", Map.of(), false, false))))
        .isInstanceOf(AiToolExecutionRejectedException.class)
        .hasMessage("User confirmation is required for AI tool: createAppointment");
  }

  @Test
  void exposesOnlyProactivelyEligibleAuthorizedTools() {
    AuthorizedAiToolGateway gateway =
        new AuthorizedAiToolGateway(
            Set.of(
                new AiToolDefinition(
                    "getSalonServices",
                    "List active salon services",
                    Set.of("client"),
                    AiToolRisk.READ_ONLY,
                    false,
                    false,
                    (context, arguments) -> "services"),
                new AiToolDefinition(
                    "createAppointment",
                    "Create an appointment",
                    Set.of("client"),
                    AiToolRisk.MUTATION,
                    true,
                    false,
                    (context, arguments) -> "created")));

    Set<String> keys =
        AiExecutionContextScope.call(
            context(Set.of("client")), gateway::proactivelyEligibleToolKeys);

    assertThat(keys).containsExactly("getSalonServices");
  }

  private static AiExecutionContext context(Set<String> roles) {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(), UUID.randomUUID(), roles, id, id, "trace-" + id, "idem-" + id);
  }
}
