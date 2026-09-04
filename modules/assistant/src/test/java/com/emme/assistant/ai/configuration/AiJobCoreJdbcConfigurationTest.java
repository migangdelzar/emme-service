package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.adapter.out.persistence.JdbcAiJobStatusStore;
import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AiJobCoreJdbcConfigurationTest {
  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(CoreJdbcDependencies.class, AiJobExecutorConfiguration.class);

  @Test
  void resolvesTheJobStoreToTheCoreJdbcClientWhenAnotherJdbcTemplateAlsoExists() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(JdbcClient.class);
          assertThat(context).hasBean("coreJdbcClient");
          assertThat(context).doesNotHaveBean("coreJdbcTemplate");
          assertThat(context.getBean(JdbcAiJobStatusStore.class)).isNotNull();
        });
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class CoreJdbcDependencies {
    @Bean(name = "coreDataSource")
    DataSource coreDataSource() {
      return new DriverManagerDataSource("jdbc:h2:mem:ai-job-core", "sa", "");
    }

    @Bean(name = "bootstrapDataSource")
    DataSource bootstrapDataSource() {
      return new DriverManagerDataSource("jdbc:h2:mem:ai-job-bootstrap", "sa", "");
    }

    @Bean(name = "competingJdbcTemplate")
    JdbcTemplate competingJdbcTemplate(DataSource bootstrapDataSource) {
      return new JdbcTemplate(bootstrapDataSource);
    }

    @Bean
    ModelExecutionScheduler modelExecutionScheduler() {
      return mock(ModelExecutionScheduler.class);
    }
  }

  @Test
  void backsOffWhenTheOptionalCoreDataSourceIsNotConfigured() {
    new ApplicationContextRunner()
        .withUserConfiguration(AiJobExecutorConfiguration.class)
        .withBean(AiJobStatusStore.class, () -> mock(AiJobStatusStore.class))
        .withBean(ModelExecutionScheduler.class, () -> mock(ModelExecutionScheduler.class))
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean("coreJdbcClient");
            });
  }
}
