package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.assistant.ai.application.port.out.ConversationWorkflowCapabilities;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SpringAiLangGraphProductionBoundaryTest {

  @Test
  void doesNotExposeTestOnlyWorkflowFactoriesInProductionTypes() {
    assertThat(
            Arrays.stream(ConversationWorkflowCapabilities.class.getDeclaredMethods())
                .map(Method::getName))
        .doesNotContain("defaults");
    assertThat(
            Arrays.stream(SpringAiLangGraphConfiguration.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("conversationWorkflowGraph"))
                .map(Method::getParameterCount))
        .doesNotContain(1);
  }
}
