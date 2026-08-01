package com.emme.catalog.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CatalogImageStoragePropertiesTest {

  @Test
  void defaultsTheImageDirectoryWhenThePropertyIsAbsent() {
    assertThat(new CatalogImageStorageProperties(null).imageDir())
        .isEqualTo("./data/catalog-images");
  }

  @Test
  void preservesConfiguredImageDirectory() {
    assertThat(new CatalogImageStorageProperties("/var/lib/emme/images").imageDir())
        .isEqualTo("/var/lib/emme/images");
  }
}
