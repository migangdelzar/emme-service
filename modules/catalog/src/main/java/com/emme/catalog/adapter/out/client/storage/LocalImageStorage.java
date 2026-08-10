package com.emme.catalog.adapter.out.client.storage;

import com.emme.catalog.application.port.out.ImageStorage;
import com.emme.catalog.configuration.CatalogImageStorageProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class LocalImageStorage implements ImageStorage {

  private final Path baseDir;

  LocalImageStorage(CatalogImageStorageProperties properties) {
    this.baseDir = Path.of(properties.imageDir());
  }

  @Override
  public String store(UUID tenantId, byte[] bytes) {
    try {
      Path dir = baseDir.resolve(tenantId.toString());
      Files.createDirectories(dir);
      Path file = dir.resolve(UUID.randomUUID() + ".img");
      Files.write(file, bytes);
      return baseDir.relativize(file).toString();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to store catalog image", e);
    }
  }
}
