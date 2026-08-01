package com.emme.tenancy.adapter.out.client.database;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.tenancy.application.port.out.DatabaseRegistryEntry;
import com.emme.tenancy.application.port.out.DatabaseRegistryPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DatabaseRegistryAdapterTest {

  private static final UUID DEFAULT_DATABASE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000000");

  @Test
  void exposesDefaultDatabaseThroughApplicationPortModel() {
    DatabaseRegistryPort port =
        new DatabaseRegistryAdapter("jdbc:h2:mem:bootstrap", "emme", "secret");

    assertThat(port.findById(DEFAULT_DATABASE_ID))
        .get()
        .isEqualTo(
            new DatabaseRegistryEntry(
                DEFAULT_DATABASE_ID, "default", "jdbc:h2:mem:bootstrap", 3, 20, 0, true));
  }
}
