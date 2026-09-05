package com.emme.assistant.ai.configuration;

import com.emme.ai.platform.learning.JdbcLearningCandidateEvaluationStore;
import com.emme.ai.platform.learning.JdbcLearningCandidateStore;
import com.emme.ai.platform.learning.LearningCandidateEvaluationRequester;
import com.emme.ai.platform.learning.LearningCandidateEvaluationStore;
import com.emme.ai.platform.learning.LearningCandidateEvaluationWorker;
import com.emme.ai.platform.learning.LearningCandidateLifecyclePolicy;
import com.emme.ai.platform.learning.LearningCandidateLifecycleService;
import com.emme.ai.platform.learning.LearningCandidatePolicy;
import com.emme.ai.platform.learning.LearningCandidateService;
import com.emme.ai.platform.learning.LearningCandidateStateStore;
import com.emme.ai.platform.learning.LearningCandidateStore;
import com.emme.assistant.ai.adapter.out.event.SpringLearningCandidateEvaluationEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Wires governed candidate capture to the existing tenant-aware JDBC boundary. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(name = "tenantJdbcClient")
public class SpringAiLearningConfiguration {

  @Bean
  @ConditionalOnMissingBean(LearningCandidateStore.class)
  LearningCandidateStore learningCandidateStore(
      @Qualifier("tenantJdbcClient") JdbcClient jdbc, ObjectMapper objectMapper) {
    return new JdbcLearningCandidateStore(jdbc, objectMapper);
  }

  @Bean
  @ConditionalOnMissingBean
  LearningCandidatePolicy learningCandidatePolicy() {
    return new LearningCandidatePolicy();
  }

  @Bean
  @ConditionalOnMissingBean
  LearningCandidateEvaluationRequester learningCandidateEvaluationRequester(
      ApplicationEventPublisher events) {
    return new SpringLearningCandidateEvaluationEventPublisher(events);
  }

  @Bean
  @ConditionalOnMissingBean(LearningCandidateEvaluationStore.class)
  LearningCandidateEvaluationStore learningCandidateEvaluationStore(
      @Qualifier("tenantJdbcClient") JdbcClient jdbc, ObjectMapper objectMapper) {
    return new JdbcLearningCandidateEvaluationStore(jdbc, objectMapper);
  }

  @Bean
  @ConditionalOnMissingBean
  LearningCandidateService learningCandidateService(
      LearningCandidatePolicy policy,
      LearningCandidateStore store,
      LearningCandidateEvaluationRequester evaluationRequester) {
    return new LearningCandidateService(policy, store, evaluationRequester);
  }

  @Bean
  @ConditionalOnMissingBean
  LearningCandidateEvaluationWorker learningCandidateEvaluationWorker(
      LearningCandidateLifecycleService lifecycle, LearningCandidateEvaluationStore evaluations) {
    return new LearningCandidateEvaluationWorker(lifecycle, evaluations);
  }

  @Bean
  @ConditionalOnMissingBean
  LearningCandidateLifecyclePolicy learningCandidateLifecyclePolicy() {
    return new LearningCandidateLifecyclePolicy();
  }

  @Bean
  @ConditionalOnMissingBean
  LearningCandidateLifecycleService learningCandidateLifecycleService(
      LearningCandidateLifecyclePolicy policy, LearningCandidateStateStore stateStore) {
    return new LearningCandidateLifecycleService(policy, stateStore);
  }
}
