package com.emme.catalog.adapter.out.client.storage;

import com.emme.catalog.application.port.out.ImageStorage;
import com.emme.catalog.configuration.CatalogImageStorageProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Optional;
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

  @Override
  public Optional<StoredImage> read(UUID tenantId, String storageKey) {
    if (tenantId == null || storageKey == null || storageKey.isBlank()) return Optional.empty();
    Path tenantRoot = baseDir.resolve(tenantId.toString()).toAbsolutePath().normalize();
    Path file = baseDir.resolve(storageKey).toAbsolutePath().normalize();
    if (!file.startsWith(tenantRoot) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
      return Optional.empty();
    try {
      if (!file.toRealPath(LinkOption.NOFOLLOW_LINKS)
          .startsWith(tenantRoot.toRealPath(LinkOption.NOFOLLOW_LINKS))) return Optional.empty();
    } catch (IOException e) {
      return Optional.empty();
    }
    try {
      String contentType = Files.probeContentType(file);
      byte[] bytes = Files.readAllBytes(file);
      return Optional.of(
          new StoredImage(
              bytes,
              contentType == null ? "application/octet-stream" : contentType,
              checksum(bytes)));
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read catalog image", e);
    }
  }

  @Override
  public void delete(UUID tenantId, String storageKey) {
    if (tenantId == null || storageKey == null) return;
    Path root = baseDir.resolve(tenantId.toString()).toAbsolutePath().normalize();
    Path file = baseDir.resolve(storageKey).toAbsolutePath().normalize();
    if (file.startsWith(root) && Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
      try {
        if (!file.toRealPath(LinkOption.NOFOLLOW_LINKS)
            .startsWith(root.toRealPath(LinkOption.NOFOLLOW_LINKS))) return;
      } catch (IOException e) {
        return;
      }
      try {
        Files.deleteIfExists(file);
      } catch (IOException e) {
        throw new UncheckedIOException("Failed to delete catalog image", e);
      }
    }
  }

  private static String checksum(byte[] bytes) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      return java.util.HexFormat.of().formatHex(digest.digest(bytes));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
