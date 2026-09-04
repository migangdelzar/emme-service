package com.emme.assistant.ai.configuration;

import com.emme.ai.contracts.model.ModelExecutionScheduler;
import com.emme.assistant.ai.adapter.out.event.SpringModulithAiJobPublisher;
import com.emme.assistant.ai.adapter.out.persistence.JdbcAiJobStatusStore;
import com.emme.assistant.ai.application.job.AiJobWorker;
import com.emme.assistant.ai.application.port.out.AiJobMetrics;
import com.emme.assistant.ai.application.port.out.AiJobStatusStore;
import com.emme.assistant.ai.application.port.out.NoopAiJobMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiJobProperties.class)
public class AiJobExecutorConfiguration {
  @Bean
  @ConditionalOnMissingBean({AiJobMetrics.class, MeterRegistry.class})
  AiJobMetrics aiJobMetrics() {
    return NoopAiJobMetrics.INSTANCE;
  }

  @Bean
  @ConditionalOnBean(name = "coreDataSource")
  SpringModulithAiJobPublisher aiJobPublisher(
      ApplicationEventPublisher events, AiJobStatusStore store) {
    return new SpringModulithAiJobPublisher(events, store);
  }

  @Bean
  @ConditionalOnBean(name = "coreDataSource")
  AiJobWorker aiJobWorker(
      AiJobStatusStore store, ModelExecutionScheduler scheduler, AiJobProperties properties) {
    return new AiJobWorker(
        store,
        scheduler,
        (request, context) -> {
          throw new IllegalStateException("AI job handler disabled/deferred: " + request.type());
        },
        properties.maxAttempts());
  }

  @Bean
  @ConditionalOnBean(name = "coreDataSource")
  @ConditionalOnMissingBean(AiJobStatusStore.class)
  JdbcAiJobStatusStore aiJobStatusStore(
      @Qualifier("coreJdbcClient") JdbcClient jdbc,
      AiJobProperties properties,
      TransactionOperations transactions,
      AiJobMetrics metrics) {
    return new JdbcAiJobStatusStore(jdbc, properties.maxAttempts(), transactions, metrics);
  }

  @Bean(name = "coreJdbcClient")
  @ConditionalOnBean(name = "coreDataSource")
  JdbcClient coreJdbcClient(@Qualifier("coreDataSource") DataSource dataSource) {
    return JdbcClient.create(dataSource);
  }

  @Bean
  @ConditionalOnBean(name = "coreDataSource")
  TransactionOperations aiJobTransactions(@Qualifier("coreDataSource") DataSource dataSource) {
    return new TransactionTemplate(new DataSourceTransactionManager(dataSource));
  }

  @Bean(name = "aiJobExecutor", destroyMethod = "shutdown")
  @ConditionalOnBean(name = "coreDataSource")
  ExecutorService aiJobExecutor(AiJobProperties p) {
    return new ThreadPoolExecutor(
        p.workerCount(),
        p.workerCount(),
        0,
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(p.queueCapacity()),
        Thread.ofPlatform().name("emme-ai-job-", 0).factory(),
        new ThreadPoolExecutor.AbortPolicy());
  }
}
