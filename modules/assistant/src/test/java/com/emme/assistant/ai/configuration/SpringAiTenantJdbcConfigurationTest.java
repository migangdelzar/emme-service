package com.emme.assistant.ai.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

class SpringAiTenantJdbcConfigurationTest {

  @Test
  void exposesTheTenantScopedJdbcClientAsThePrimaryClient() throws Exception {
    Method method =
        SpringAiTenantJdbcConfiguration.class.getDeclaredMethod(
            "tenantJdbcClient", DataSource.class);

    assertThat(method.getAnnotation(Bean.class).name()).containsExactly("tenantJdbcClient");
    assertThat(method.getAnnotation(Primary.class)).isNotNull();
    assertThat(method.getParameters()[0].getAnnotation(Qualifier.class).value())
        .isEqualTo("tenantScopedDataSource");
    assertThat(new SpringAiTenantJdbcConfiguration().tenantJdbcClient(mock(DataSource.class)))
        .isNotNull();
  }
}
