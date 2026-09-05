package com.emme.tenancy.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

@SuppressWarnings("unchecked")
class BootstrapJdbcConfigurationTest {

  @Test
  void reusesTheCoreDataSourceWhenNoStandaloneBootstrapUrlIsConfigured() {
    DataSource coreDataSource = mock(DataSource.class);
    ObjectProvider<DataSource> coreDataSourceProvider = mock(ObjectProvider.class);
    when(coreDataSourceProvider.getIfAvailable(org.mockito.ArgumentMatchers.any()))
        .thenReturn(coreDataSource);

    DataSource bootstrapDataSource =
        new BootstrapJdbcConfiguration()
            .bootstrapJdbcDataSource("", "", "", coreDataSourceProvider);

    assertThat(bootstrapDataSource).isSameAs(coreDataSource);
  }
}
