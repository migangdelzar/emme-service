package com.emme.ai.platform.configuration;

import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.ai.platform.model.BoundedModelExecutionScheduler;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition root for provider configuration and transport dependencies. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({AiProviderProperties.class, ModelAdmissionProperties.class})
public class AiProviderConfiguration {

  @Bean
  AiProviderHttpClient aiProviderHttpClient() {
    return new AiProviderHttpClient(new OkHttpClient());
  }

  @Bean
  @ConditionalOnMissingBean(ModelExecutionScheduler.class)
  ModelExecutionScheduler modelExecutionScheduler(ModelAdmissionProperties properties) {
    return new BoundedModelExecutionScheduler(properties.profile());
  }
}
