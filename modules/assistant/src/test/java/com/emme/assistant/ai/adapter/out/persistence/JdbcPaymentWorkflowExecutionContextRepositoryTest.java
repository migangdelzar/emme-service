package com.emme.assistant.ai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

class JdbcPaymentWorkflowExecutionContextRepositoryTest {

  @Test
  void isOptInWithTheLangGraphWorkflowBoundary() {
    assertThat(JdbcPaymentWorkflowExecutionContextRepository.class)
        .hasAnnotation(ConditionalOnProperty.class);
    assertThat(
            JdbcPaymentWorkflowExecutionContextRepository.class
                .getAnnotation(ConditionalOnProperty.class)
                .prefix())
        .isEqualTo("app.ai.langgraph");
  }
}
