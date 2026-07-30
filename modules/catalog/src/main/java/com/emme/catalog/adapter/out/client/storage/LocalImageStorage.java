package com.emme.catalog.adapter.out.client.storage;

import com.emme.catalog.application.port.out.ImageStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class LocalImageStorage implements ImageStorage {

  private final Path baseDir;

  LocalImageStorage(@Value("${app.catalog.image-dir:./data/catalog-images}") String baseDir) {
    this.baseDir = Path.of(baseDir);
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
