package com.emme.assistant.ai.configuration;

import com.emme.ai.contracts.graph.KnowledgeGraphProjector;
import com.emme.ai.contracts.graph.KnowledgeGraphRetriever;
import com.emme.assistant.ai.adapter.out.graph.AgeGraphAdapter;
import com.emme.assistant.ai.adapter.out.graph.AgeGraphClient;
import com.emme.assistant.ai.adapter.out.graph.JdbcAgeGraphClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/** Opt-in wiring for the PostgreSQL/Apache AGE derived graph. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SpringAiAgeProperties.class)
@ConditionalOnProperty(prefix = "app.ai.age", name = "enabled", havingValue = "true")
@ConditionalOnBean(name = "tenantScopedDataSource")
public class SpringAiAgeConfiguration {

  @Bean
  @ConditionalOnMissingBean
  AgeGraphClient ageGraphClient(
      @Qualifier("tenantScopedDataSource") DataSource tenantDataSource,
      @Qualifier("aiTenantJdbcClient") JdbcClient tenantJdbc,
      ObjectMapper objectMapper) {
    TransactionOperations transactions =
        new TransactionTemplate(new DataSourceTransactionManager(tenantDataSource));
    return new JdbcAgeGraphClient(tenantJdbc, objectMapper, transactions);
  }

  @Bean
  @ConditionalOnMissingBean(AgeGraphAdapter.class)
  AgeGraphAdapter ageGraphAdapter(AgeGraphClient client, SpringAiAgeProperties properties) {
    return new AgeGraphAdapter(client, properties);
  }

  @Bean
  @ConditionalOnMissingBean(KnowledgeGraphProjector.class)
  KnowledgeGraphProjector knowledgeGraphProjector(AgeGraphAdapter adapter) {
    return adapter;
  }

  @Bean
  @ConditionalOnMissingBean(KnowledgeGraphRetriever.class)
  KnowledgeGraphRetriever knowledgeGraphRetriever(AgeGraphAdapter adapter) {
    return adapter;
  }
}
