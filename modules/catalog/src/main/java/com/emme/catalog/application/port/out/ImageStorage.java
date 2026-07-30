package com.emme.catalog.application.port.out;

import java.util.UUID;

/** Stores reference images; phase 1 is local filesystem, later S3-compatible. */
public interface ImageStorage {
  String store(UUID tenantId, byte[] bytes);
}
