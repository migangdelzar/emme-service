package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AiJobPropertiesTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(BindingConfiguration.class);

  @Test
  void bindsConfiguredValuesThroughTheCanonicalConstructor() {
    contextRunner
        .withPropertyValues(
            "app.ai.jobs.worker-count=4",
            "app.ai.jobs.queue-capacity=64",
            "app.ai.jobs.max-attempts=5",
            "app.ai.jobs.poll-limit=12")
        .run(
            context ->
                assertThat(context.getBean(AiJobProperties.class))
                    .isEqualTo(new AiJobProperties(4, 64, 5, 12)));
  }

  @Test
  void preservesSafeDefaultsWhenJobPropertiesAreAbsent() {
    contextRunner.run(
        context ->
            assertThat(context.getBean(AiJobProperties.class))
                .isEqualTo(new AiJobProperties(2, 32, 3, 32)));
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(AiJobProperties.class)
  static class BindingConfiguration {}
}
