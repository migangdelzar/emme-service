package com.emme.tenancy.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@SuppressWarnings("unchecked")
class BootstrapJdbcConfigurationTest {

  @Test
  void reusesTheCoreDataSourceWhenNoStandaloneBootstrapUrlIsConfigured() {
    DataSource coreDataSource = mock(DataSource.class);
    ObjectProvider<JdbcConnectionDetails> detailsProvider = mock(ObjectProvider.class);
    ObjectProvider<DataSource> coreDataSourceProvider = mock(ObjectProvider.class);
    when(coreDataSourceProvider.getIfAvailable(org.mockito.ArgumentMatchers.any()))
        .thenReturn(coreDataSource);

    DataSource bootstrapDataSource =
        new BootstrapJdbcConfiguration()
            .bootstrapJdbcDataSource("", "", "", detailsProvider, coreDataSourceProvider);

    assertThat(bootstrapDataSource).isSameAs(coreDataSource);
  }

  @Test
  void usesServiceConnectionDetailsForTheBootstrapDataSource() {
    JdbcConnectionDetails details = mock(JdbcConnectionDetails.class);
    when(details.getJdbcUrl()).thenReturn("jdbc:postgresql://localhost:5432/emme_test");
    when(details.getUsername()).thenReturn("emme");
    when(details.getPassword()).thenReturn("secret");
    ObjectProvider<JdbcConnectionDetails> detailsProvider = mock(ObjectProvider.class);
    when(detailsProvider.getIfAvailable()).thenReturn(details);
    ObjectProvider<DataSource> coreDataSourceProvider = mock(ObjectProvider.class);

    DataSource bootstrapDataSource =
        new BootstrapJdbcConfiguration()
            .bootstrapJdbcDataSource("", "", "", detailsProvider, coreDataSourceProvider);

    assertThat(bootstrapDataSource).isInstanceOf(DriverManagerDataSource.class);
    var driverManagerDataSource = (DriverManagerDataSource) bootstrapDataSource;
    assertThat(driverManagerDataSource.getUrl())
        .isEqualTo("jdbc:postgresql://localhost:5432/emme_test");
    assertThat(driverManagerDataSource.getUsername()).isEqualTo("emme");
    assertThat(driverManagerDataSource.getPassword()).isEqualTo("secret");
  }
}
