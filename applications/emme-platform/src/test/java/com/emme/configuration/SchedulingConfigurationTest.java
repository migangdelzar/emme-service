package com.emme.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
import com.emme.assistant.ai.configuration.AiJobExecutorConfiguration;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

class SchedulingConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(SchedulingConfiguration.class);

  @Test
  void disablesScheduledTasksWhenSchedulingIsDisabled() {
    contextRunner
        .withPropertyValues("spring.task.scheduling.enabled=false")
        .run(
            context ->
                assertThat(context).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class));
  }

  @Test
  void enablesScheduledTasksByDefaultForProduction() {
    contextRunner.run(
        context -> assertThat(context).hasSingleBean(ScheduledAnnotationBeanPostProcessor.class));
  }

  @Test
  void disablesAiJobSchedulingWhenSchedulingIsDisabled() {
    aiJobContextRunner()
        .withPropertyValues("spring.task.scheduling.enabled=false")
        .run(
            context ->
                assertThat(context).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class));
  }

  @Test
  void enablesAiJobSchedulingWhenSchedulingIsEnabled() {
    aiJobContextRunner()
        .withPropertyValues("spring.task.scheduling.enabled=true")
        .run(
            context ->
                assertThat(context).hasSingleBean(ScheduledAnnotationBeanPostProcessor.class));
  }

  private static ApplicationContextRunner aiJobContextRunner() {
    return new ApplicationContextRunner()
        .withUserConfiguration(SchedulingConfiguration.class, AiJobExecutorConfiguration.class)
        .withUserConfiguration(AiJobDependencies.class);
  }

  @TestConfiguration(proxyBeanMethods = false)
  @Import(AiJobExecutorConfiguration.class)
  static class AiJobDependencies {
    @Bean(name = "coreDataSource")
    DataSource coreDataSource() {
      return new DriverManagerDataSource("jdbc:h2:mem:ai-job-scheduling", "sa", "");
    }

    @Bean
    AiJobStatusStore aiJobStatusStore() {
      return mock(AiJobStatusStore.class);
    }

    @Bean
    ModelExecutionScheduler modelExecutionScheduler() {
      return mock(ModelExecutionScheduler.class);
    }
  }
}
