package com.emme.assistant.ai.configuration;

import com.emme.assistant.ai.adapter.out.observability.MicrometerSemanticMetrics;
import com.emme.assistant.ai.application.port.out.NoopSemanticMetrics;
import com.emme.assistant.ai.application.port.out.SemanticMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Selects Micrometer semantic metrics when available and otherwise uses a no-op. */
@Configuration(proxyBeanMethods = false)
public class SpringAiSemanticMetricsConfiguration {

  @Bean
  @ConditionalOnBean(MeterRegistry.class)
  @ConditionalOnMissingBean(SemanticMetrics.class)
  SemanticMetrics micrometerSemanticMetrics(MeterRegistry registry) {
    return new MicrometerSemanticMetrics(registry);
  }

  @Bean
  @ConditionalOnMissingBean({SemanticMetrics.class, MeterRegistry.class})
  SemanticMetrics noopSemanticMetrics() {
    return NoopSemanticMetrics.INSTANCE;
  }
}
