package com.emme.assistant.ai.configuration;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/** Composition root for the mandatory Spring AI request-context advisors. */
@Configuration(proxyBeanMethods = false)
public class SpringAiAdvisorConfiguration {

  static List<Advisor> orderedAdvisors(List<? extends Advisor> advisors) {
    var ordered = new ArrayList<Advisor>(List.copyOf(advisors));
    AnnotationAwareOrderComparator.sort(ordered);
    return List.copyOf(ordered);
  }

  @Bean
  @ConditionalOnMissingBean
  TenantSecurityAdvisor tenantSecurityAdvisor() {
    return new TenantSecurityAdvisor();
  }

  @Bean
  @ConditionalOnMissingBean
  PromptVersionAdvisor promptVersionAdvisor() {
    return new PromptVersionAdvisor("chat-v1");
  }

  @Bean
  @ConditionalOnMissingBean
  InputGuard inputGuard() {
    return new DefaultInputGuard(64 * 1024L, 10);
  }

  @Bean
  @ConditionalOnMissingBean
  ContextGuard contextGuard() {
    return new DefaultContextGuard(java.time.Clock.systemUTC());
  }

  @Bean
  @ConditionalOnMissingBean
  ToolGuard toolGuard(List<AiToolDefinition> definitions) {
    Set<String> allowedKeys =
        definitions.stream()
            .map(AiToolDefinition::key)
            .collect(java.util.stream.Collectors.toSet());
    Set<String> staffOnlyKeys =
        definitions.stream()
            .filter(AiToolDefinition::staffApprovalRequired)
            .map(AiToolDefinition::key)
            .collect(java.util.stream.Collectors.toSet());
    return new DefaultToolGuard(allowedKeys, staffOnlyKeys);
  }

  @Bean
  @ConditionalOnMissingBean
  GroundingGuard groundingGuard() {
    return new DefaultGroundingGuard();
  }

  @Bean
  @ConditionalOnMissingBean
  OutputGuard outputGuard() {
    return new DefaultOutputGuard(Set.of("web", "whatsapp", "internal"));
  }

  @Bean
  @ConditionalOnMissingBean
  DeliveryGuard deliveryGuard() {
    return new DefaultDeliveryGuard(Set.of("web", "whatsapp", "internal"));
  }

  @Bean
  @ConditionalOnMissingBean
  GuardrailPipeline guardrailPipeline(
      InputGuard input,
      ContextGuard context,
      ToolGuard tool,
      GroundingGuard grounding,
      OutputGuard output,
      DeliveryGuard delivery) {
    return new com.emme.assistant.ai.application.guardrail.DefaultGuardrailPipeline(
        input, context, tool, grounding, output, delivery);
  }

  @Bean
  @ConditionalOnMissingBean
  InputGuardAdvisor inputGuardAdvisor(InputGuard inputGuard) {
    return new InputGuardAdvisor(inputGuard);
  }

  @Bean
  @ConditionalOnMissingBean
  OutputGuardAdvisor outputGuardAdvisor(OutputGuard outputGuard) {
    return new OutputGuardAdvisor(outputGuard);
  }
}
