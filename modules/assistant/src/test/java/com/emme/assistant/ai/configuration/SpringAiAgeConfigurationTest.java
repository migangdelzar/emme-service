package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.assistant.ai.adapter.out.graph.AgeGraphAdapter;
import com.emme.assistant.ai.adapter.out.graph.AgeGraphClient;
import com.emme.assistant.ai.adapter.out.graph.JdbcAgeGraphClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;

class SpringAiAgeConfigurationTest {

  @Test
  void providesSafeDisabledDefaults() {
    SpringAiAgeProperties properties = new SpringAiAgeProperties(false, null, 0);

    assertThat(properties.enabled()).isFalse();
    assertThat(properties.graphPrefix()).isEqualTo("emme_ai_graph_");
    assertThat(properties.retrievalLimit()).isEqualTo(5);
  }

  @Test
  void wiresTheJdbcClientBehindTheProviderNeutralGraphContracts() {
    SpringAiAgeConfiguration configuration = new SpringAiAgeConfiguration();
    DataSource dataSource = mock(DataSource.class);
    AgeGraphClient client =
        configuration.ageGraphClient(
            dataSource,
            new SpringAiTenantJdbcConfiguration().aiTenantJdbcClient(dataSource),
            new ObjectMapper());
    AgeGraphAdapter adapter =
        configuration.ageGraphAdapter(client, new SpringAiAgeProperties(true, "emme_ai_graph_", 5));

    assertThat(client).isInstanceOf(JdbcAgeGraphClient.class);
    assertThat(adapter).isNotNull();
  }

  @Test
  void reusesTheCanonicalTenantJdbcClientBean() throws Exception {
    Method method =
        SpringAiAgeConfiguration.class.getDeclaredMethod(
            "ageGraphClient", DataSource.class, JdbcClient.class, ObjectMapper.class);

    assertThat(method.getParameters()[1].getAnnotation(Qualifier.class).value())
        .isEqualTo("aiTenantJdbcClient");
    assertThat(SpringAiAgeConfiguration.class.getDeclaredMethods())
        .extracting(Method::getName)
        .doesNotContain("tenantScopedJdbcClient");
  }
}
