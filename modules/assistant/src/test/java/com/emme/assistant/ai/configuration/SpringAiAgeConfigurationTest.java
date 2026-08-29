package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.emme.assistant.ai.adapter.out.graph.AgeGraphAdapter;
import com.emme.assistant.ai.adapter.out.graph.AgeGraphClient;
import com.emme.assistant.ai.adapter.out.graph.JdbcAgeGraphClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

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
    AgeGraphClient client =
        configuration.ageGraphClient(mock(DataSource.class), new ObjectMapper());
    AgeGraphAdapter adapter =
        configuration.ageGraphAdapter(client, new SpringAiAgeProperties(true, "emme_ai_graph_", 5));

    assertThat(client).isInstanceOf(JdbcAgeGraphClient.class);
    assertThat(adapter).isNotNull();
  }
}
