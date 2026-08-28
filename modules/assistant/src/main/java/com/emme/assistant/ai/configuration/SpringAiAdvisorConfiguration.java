package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.adapter.out.provider.springai.advisor.PromptVersionAdvisor;
import com.emme.assistant.ai.adapter.out.provider.springai.advisor.TenantSecurityAdvisor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root for the mandatory Spring AI request-context advisors. */
@Configuration(proxyBeanMethods = false)
public class SpringAiAdvisorConfiguration {

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
