package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.emme.assistant.ai.adapter.out.provider.springai.advisor.GroundingGuardAdvisor;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.InputGuardAdvisor;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.OutputGuardAdvisor;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.PromptVersionAdvisor;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.TenantSecurityAdvisor;
import com.emme.assistant.ai.application.guardrail.ContextGuard;
import com.emme.assistant.ai.application.guardrail.DefaultContextGuard;
import com.emme.assistant.ai.application.guardrail.DefaultDeliveryGuard;
import com.emme.assistant.ai.application.guardrail.DefaultGroundingGuard;
import com.emme.assistant.ai.application.guardrail.DefaultInputGuard;
import com.emme.assistant.ai.application.guardrail.DefaultOutputGuard;
import com.emme.assistant.ai.application.guardrail.DefaultToolGuard;
import com.emme.assistant.ai.application.guardrail.DeliveryGuard;
import com.emme.assistant.ai.application.guardrail.GroundingGuard;
import com.emme.assistant.ai.application.guardrail.GuardrailPipeline;
import com.emme.assistant.ai.application.guardrail.InputGuard;
import com.emme.assistant.ai.application.guardrail.OutputGuard;
import com.emme.assistant.ai.application.guardrail.ToolGuard;
import com.emme.assistant.ai.application.tool.AiToolDefinition;
import com.emme.assistant.ai.application.tool.AiToolRisk;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.api.Advisor;

class SpringAiAdvisorConfigurationTest {

  @Test
  void createsTheSecurityAndPromptAdvisorsForModelClients() {
    SpringAiAdvisorConfiguration configuration = new SpringAiAdvisorConfiguration();

    assertThat(configuration.tenantSecurityAdvisor()).isInstanceOf(TenantSecurityAdvisor.class);
    assertThat(configuration.promptVersionAdvisor()).isInstanceOf(PromptVersionAdvisor.class);
  }

  @Test
  void ordersTheAdvisorChainBySpringAiPrecedenceInsteadOfAssemblyOrder() {
    SpringAiAdvisorConfiguration configuration = new SpringAiAdvisorConfiguration();
    TenantSecurityAdvisor security = configuration.tenantSecurityAdvisor();
    PromptVersionAdvisor prompt = configuration.promptVersionAdvisor();
    Advisor retrieval = mock(Advisor.class);
    when(retrieval.getOrder()).thenReturn(0);

    assertThat(SpringAiAdvisorConfiguration.orderedAdvisors(List.of(retrieval, prompt, security)))
        .containsExactly(security, prompt, retrieval);
  }

  @Test
  void composesTheDefaultGuardrailPipelineFromTypedBoundaries() {
    SpringAiAdvisorConfiguration configuration = new SpringAiAdvisorConfiguration();
    InputGuard input = configuration.inputGuard();
    ContextGuard context = configuration.contextGuard();
    ToolGuard tool = configuration.toolGuard(List.of(toolDefinition()));
    GroundingGuard grounding = configuration.groundingGuard();
    OutputGuard output = configuration.outputGuard();
    DeliveryGuard delivery = configuration.deliveryGuard();

    assertThat(input).isInstanceOf(DefaultInputGuard.class);
    assertThat(context).isInstanceOf(DefaultContextGuard.class);
    assertThat(tool).isInstanceOf(DefaultToolGuard.class);
    assertThat(grounding).isInstanceOf(DefaultGroundingGuard.class);
    assertThat(output).isInstanceOf(DefaultOutputGuard.class);
    assertThat(delivery).isInstanceOf(DefaultDeliveryGuard.class);
    assertThat(configuration.guardrailPipeline(input, context, tool, grounding, output, delivery))
        .isInstanceOf(GuardrailPipeline.class);
  }

  @Test
  void exposesInputAndOutputAdvisorsWithGuardrailPrecedence() {
    SpringAiAdvisorConfiguration configuration = new SpringAiAdvisorConfiguration();
    InputGuard input = configuration.inputGuard();
    OutputGuard output = configuration.outputGuard();

    InputGuardAdvisor inputAdvisor = configuration.inputGuardAdvisor(input);
    OutputGuardAdvisor outputAdvisor = configuration.outputGuardAdvisor(output);

    assertThat(inputAdvisor.getOrder()).isLessThan(configuration.promptVersionAdvisor().getOrder());
    assertThat(outputAdvisor.getOrder())
        .isGreaterThan(configuration.promptVersionAdvisor().getOrder());
  }

  @Test
  void composesTheSpringAiGroundingAdvisorFromTheTypedGuard() {
    SpringAiAdvisorConfiguration configuration = new SpringAiAdvisorConfiguration();

    assertThat(configuration.groundingGuardAdvisor(configuration.groundingGuard()))
        .isInstanceOf(GroundingGuardAdvisor.class);
  }

  private static AiToolDefinition toolDefinition() {
    return new AiToolDefinition(
        "faq.read",
        "Read FAQs",
        Set.of("client"),
        AiToolRisk.READ_ONLY,
        false,
        false,
        (context, arguments) -> "answer");
  }
}
