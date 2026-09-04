package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.adapter.out.provider.springai.advisor.PromptVersionAdvisor;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.TenantSecurityAdvisor;
import java.util.ArrayList;
import java.util.List;
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
}
