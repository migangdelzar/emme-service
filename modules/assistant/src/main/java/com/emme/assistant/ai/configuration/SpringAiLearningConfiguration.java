package com.emme.assistant.ai.configuration;

import com.emme.ai.platform.learning.JdbcLearningCandidateStore;
import com.emme.ai.platform.learning.LearningCandidatePolicy;
import com.emme.ai.platform.learning.LearningCandidateService;
import com.emme.ai.platform.learning.LearningCandidateStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Wires governed candidate capture to the existing tenant-aware JDBC boundary. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(name = "aiTenantJdbcClient")
public class SpringAiLearningConfiguration {

  @Bean
  @ConditionalOnMissingBean(LearningCandidateStore.class)
  LearningCandidateStore learningCandidateStore(
      @Qualifier("aiTenantJdbcClient") JdbcClient jdbc, ObjectMapper objectMapper) {
    return new JdbcLearningCandidateStore(jdbc, objectMapper);
  }

  @Bean
  @ConditionalOnMissingBean
  LearningCandidatePolicy learningCandidatePolicy() {
    return new LearningCandidatePolicy();
  }

  @Bean
  @ConditionalOnMissingBean
  LearningCandidateService learningCandidateService(
      LearningCandidatePolicy policy, LearningCandidateStore store) {
    return new LearningCandidateService(policy, store);
  }
}
