package com.emme.catalog.adapter.out.client.storage;

import com.emme.catalog.application.port.out.ImageStorage;
import com.emme.catalog.configuration.CatalogImageStorageProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.Optional;
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

  @Override
  public Optional<StoredImage> read(UUID tenantId, String storageKey) {
    if (tenantId == null || storageKey == null || storageKey.isBlank()) return Optional.empty();
    Path tenantRoot = baseDir.resolve(tenantId.toString()).normalize();
    Path file = baseDir.resolve(storageKey).normalize();
    if (!file.startsWith(tenantRoot) || !Files.isRegularFile(file)) return Optional.empty();
    try {
      String contentType = Files.probeContentType(file);
      return Optional.of(new StoredImage(Files.readAllBytes(file), contentType == null ? "application/octet-stream" : contentType));
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read catalog image", e);
    }
  }
}
