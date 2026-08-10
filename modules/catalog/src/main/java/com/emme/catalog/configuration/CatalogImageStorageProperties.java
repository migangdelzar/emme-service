package com.emme.catalog.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed Catalog image-storage settings bound to {@code app.catalog.*}. */
@ConfigurationProperties(prefix = "app.catalog")
public record CatalogImageStorageProperties(String imageDir) {

  public CatalogImageStorageProperties {
    imageDir = imageDir == null ? "./data/catalog-images" : imageDir;
  }
}
