package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.adapter.out.observability.MicrometerAiJobMetrics;
import com.emme.assistant.ai.adapter.out.observability.MicrometerAiWorkflowObserver;
import com.emme.assistant.ai.application.port.out.AiJobMetrics;
import com.emme.assistant.ai.application.port.out.AiWorkflowObserver;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers AI workflow metrics when the host application exposes Micrometer. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(MeterRegistry.class)
public class SpringAiObservabilityConfiguration {

  @Bean
  @ConditionalOnMissingBean
  AiWorkflowObserver aiWorkflowObserver(MeterRegistry registry) {
    return new MicrometerAiWorkflowObserver(registry);
  }

  @Bean
  @ConditionalOnMissingBean
  AiJobMetrics aiJobMetrics(MeterRegistry registry) {
    return new MicrometerAiJobMetrics(registry);
  }
}
