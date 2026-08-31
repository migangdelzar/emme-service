package com.emme.catalog.application.port.out;

import java.util.UUID;
import java.util.Optional;
import com.emme.ai.contracts.image.TenantImageReader;
import com.emme.ai.contracts.image.TenantImageWriter;

/** Stores reference images; phase 1 is local filesystem, later S3-compatible. */
public interface ImageStorage extends TenantImageReader, TenantImageWriter {
  String store(UUID tenantId, byte[] bytes);

  @Override
  default Optional<TenantImageReader.StoredImage> read(UUID tenantId, String storageKey) {
    return Optional.empty();
  }

}
