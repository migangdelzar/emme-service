package com.emme.assistant.ai.application.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.application.trace.AiToolCallStatus;
import com.emme.assistant.ai.application.trace.AiToolCallTrace;
import com.emme.assistant.ai.application.trace.AiTraceRecorder;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

  @Test
  void recordsSuccessfulToolCallsWithTheAuthorizationOutcome() {
    AiTraceRecorder recorder = org.mockito.Mockito.mock(AiTraceRecorder.class);
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
                    (context, arguments) -> "services")),
            recorder);

    AiExecutionContextScope.call(
        context(Set.of("client")),
        () -> gateway.execute(new AiToolInvocation("getSalonServices", Map.of(), false, false)));

    ArgumentCaptor<AiToolCallTrace> trace = ArgumentCaptor.forClass(AiToolCallTrace.class);
    org.mockito.Mockito.verify(recorder).recordToolCall(trace.capture());
    assertThat(trace.getValue().status()).isEqualTo(AiToolCallStatus.SUCCEEDED);
    assertThat(trace.getValue().authorized()).isTrue();
    assertThat(trace.getValue().riskLevel()).isEqualTo("READ_ONLY");
  }

  @Test
  void recordsRejectedToolCallsWithoutInvokingTheHandler() {
    AiTraceRecorder recorder = org.mockito.Mockito.mock(AiTraceRecorder.class);
    AiToolHandler handler = org.mockito.Mockito.mock(AiToolHandler.class);
    AuthorizedAiToolGateway gateway =
        new AuthorizedAiToolGateway(
            Set.of(
                new AiToolDefinition(
                    "createAppointment",
                    "Create an appointment",
                    Set.of("tenant_staff"),
                    AiToolRisk.MUTATION,
                    true,
                    false,
                    handler)),
            recorder);

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    context(Set.of("client")),
                    () ->
                        gateway.execute(
                            new AiToolInvocation("createAppointment", Map.of(), false, false))))
        .isInstanceOf(AiToolExecutionRejectedException.class);

    ArgumentCaptor<AiToolCallTrace> trace = ArgumentCaptor.forClass(AiToolCallTrace.class);
    org.mockito.Mockito.verify(recorder).recordToolCall(trace.capture());
    assertThat(trace.getValue().status()).isEqualTo(AiToolCallStatus.REJECTED);
    assertThat(trace.getValue().authorized()).isFalse();
    org.mockito.Mockito.verifyNoInteractions(handler);
  }

  @Test
  void doesNotMakeToolExecutionFailWhenTracePersistenceFails() {
    AiTraceRecorder recorder = org.mockito.Mockito.mock(AiTraceRecorder.class);
    org.mockito.Mockito.doThrow(new IllegalStateException("database down"))
        .when(recorder)
        .recordToolCall(org.mockito.ArgumentMatchers.any());
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
                    (context, arguments) -> "services")),
            recorder);

    assertThat(
            AiExecutionContextScope.call(
                context(Set.of("client")),
                () ->
                    gateway.execute(
                        new AiToolInvocation("getSalonServices", Map.of(), false, false))))
        .isEqualTo(new AiToolResult("getSalonServices", "services", true));
  }

  @Test
  void rejectsToolArgumentsOutsideTheBackendDeclaredSchema() {
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
                    (context, arguments) -> "slots",
                    Set.of("serviceId", "date"),
                    Set.of("serviceId", "date"))));

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    context(Set.of("client")),
                    () ->
                        gateway.execute(
                            new AiToolInvocation(
                                "findAvailability", Map.of("serviceId", "service"), false, false))))
        .isInstanceOf(AiToolExecutionRejectedException.class)
        .hasMessage("Missing required AI tool argument: date");

    assertThatThrownBy(
            () ->
                AiExecutionContextScope.call(
                    context(Set.of("client")),
                    () ->
                        gateway.execute(
                            new AiToolInvocation(
                                "findAvailability",
                                Map.of(
                                    "serviceId",
                                    "service",
                                    "date",
                                    "2026-08-29",
                                    "tenantId",
                                    "other"),
                                false,
                                false))))
        .isInstanceOf(AiToolExecutionRejectedException.class)
        .hasMessage("Unknown AI tool argument: tenantId");
  }

  private static AiExecutionContext context(Set<String> roles) {
    UUID id = UUID.randomUUID();
    return new AiExecutionContext(
        UUID.randomUUID(), UUID.randomUUID(), roles, id, id, "trace-" + id, "idem-" + id);
  }
}
