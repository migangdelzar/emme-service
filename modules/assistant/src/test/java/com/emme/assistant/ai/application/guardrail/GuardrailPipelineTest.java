package com.emme.assistant.ai.application.guardrail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.guardrail.ContextRequest;
import com.emme.ai.contracts.guardrail.DeliveryRequest;
import com.emme.ai.contracts.guardrail.GroundingRequest;
import com.emme.ai.contracts.guardrail.GuardrailAction;
import com.emme.ai.contracts.guardrail.GuardrailDecision;
import com.emme.ai.contracts.guardrail.GuardrailRequest;
import com.emme.ai.contracts.guardrail.InputRequest;
import com.emme.ai.contracts.guardrail.OutputRequest;
import com.emme.ai.contracts.guardrail.ToolRequest;
import com.emme.kernel.context.AiExecutionContext;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GuardrailPipelineTest {

  private static final AiExecutionContext CONTEXT =
      new AiExecutionContext(
          UUID.randomUUID(),
          UUID.randomUUID(),
          Set.of("tenant_client"),
          UUID.randomUUID(),
          UUID.randomUUID(),
          "trace-1",
          "idempotency-1");

  @Test
  void stopsAtTheFirstBlockingDecision() {
    InputGuard input = mock(InputGuard.class);
    ContextGuard context = mock(ContextGuard.class);
    ToolGuard tool = mock(ToolGuard.class);
    GroundingGuard grounding = mock(GroundingGuard.class);
    OutputGuard output = mock(OutputGuard.class);
    DeliveryGuard delivery = mock(DeliveryGuard.class);
    GuardrailDecision blocked =
        new GuardrailDecision(GuardrailAction.BLOCK, "input.blocked", Map.of("stage", "input"));
    when(input.check(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(CONTEXT)))
        .thenReturn(blocked);

    GuardrailPipeline pipeline =
        new DefaultGuardrailPipeline(input, context, tool, grounding, output, delivery);

    assertThat(pipeline.evaluate(request(), CONTEXT)).isEqualTo(blocked);
    verifyNoInteractions(context, tool, grounding, output, delivery);
  }

  @Test
  void evaluatesTheRemainingStagesWhenInputAndContextAreAllowed() {
    InputGuard input = mock(InputGuard.class);
    ContextGuard context = mock(ContextGuard.class);
    ToolGuard tool = mock(ToolGuard.class);
    GroundingGuard grounding = mock(GroundingGuard.class);
    OutputGuard output = mock(OutputGuard.class);
    DeliveryGuard delivery = mock(DeliveryGuard.class);
    GuardrailDecision allowed = new GuardrailDecision(GuardrailAction.ALLOW, "allowed", Map.of());
    GuardrailDecision delivered =
        new GuardrailDecision(GuardrailAction.DELIVER, "delivery.allowed", Map.of());
    when(input.check(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(CONTEXT)))
        .thenReturn(allowed);
    when(context.check(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(CONTEXT)))
        .thenReturn(allowed);
    when(tool.check(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(CONTEXT)))
        .thenReturn(allowed);
    when(grounding.check(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(CONTEXT)))
        .thenReturn(allowed);
    when(output.check(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(CONTEXT)))
        .thenReturn(allowed);
    when(delivery.check(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(CONTEXT)))
        .thenReturn(delivered);

    GuardrailPipeline pipeline =
        new DefaultGuardrailPipeline(input, context, tool, grounding, output, delivery);

    assertThat(pipeline.evaluate(request(), CONTEXT)).isEqualTo(delivered);
  }

  private static GuardrailRequest request() {
    return new GuardrailRequest(
        new InputRequest("hello", 5, 0, "idempotency-1"),
        new ContextRequest(
            CONTEXT.tenantId(),
            CONTEXT.principalId(),
            CONTEXT.roles(),
            CONTEXT.traceId(),
            Instant.now().plusSeconds(30)),
        new ToolRequest("faq.read", Map.of(), false, false, "idempotency-1"),
        new GroundingRequest(true, 0.9, 0.2, java.util.List.of("doc-1")),
        new OutputRequest("web", "hello", false, false),
        new DeliveryRequest("web", "hello", 1000, false));
  }
}
