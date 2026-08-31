package com.emme.ai.contracts.image;

import java.util.Optional;
import java.util.UUID;

/** Shared tenant-scoped image read contract used across bounded contexts. */
public interface TenantImageReader {
  Optional<StoredImage> read(UUID tenantId, String storageKey);

  record StoredImage(byte[] bytes, String mediaType, String checksum) {
    public StoredImage(byte[] bytes, String mediaType) { this(bytes, mediaType, ""); }
  }
}
