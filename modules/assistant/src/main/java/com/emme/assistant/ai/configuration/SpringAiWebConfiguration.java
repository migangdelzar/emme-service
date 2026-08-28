package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.adapter.in.web.security.AiWebExecutionContextFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root for authenticated AI web request context creation. */
@Configuration(proxyBeanMethods = false)
public class SpringAiWebConfiguration {

  @Bean
  @ConditionalOnMissingBean
  AiWebExecutionContextFactory aiWebExecutionContextFactory() {
    return new AiWebExecutionContextFactory();
  }
}
