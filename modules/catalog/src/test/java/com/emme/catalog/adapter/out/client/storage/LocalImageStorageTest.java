package com.emme.catalog.adapter.out.client.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.emme.catalog.configuration.CatalogImageStorageProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LocalImageStorageTest {
  @Test
  void doesNotReadOrDeleteSymlinkOutsideTenantDirectory() throws Exception {
    Path base = Files.createTempDirectory("catalog-images");
    Path outside = Files.createTempFile("outside", ".img");
    Files.writeString(outside, "secret");
    UUID tenant = UUID.randomUUID();
    Path tenantDir = Files.createDirectories(base.resolve(tenant.toString()));
    Path link = tenantDir.resolve("link.img");
    Files.createSymbolicLink(link, outside);

    var storage = new LocalImageStorage(new CatalogImageStorageProperties(base.toString()));

    assertThat(storage.read(tenant, tenant + "/link.img")).isEmpty();
    storage.delete(tenant, tenant + "/link.img");
    assertThat(Files.exists(outside)).isTrue();
    assertThat(Files.exists(link)).isTrue();
  }
}
