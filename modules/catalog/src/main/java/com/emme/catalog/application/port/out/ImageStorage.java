package com.emme.catalog.application.port.out;

import com.emme.ai.contracts.image.TenantImageReader;
import com.emme.ai.contracts.image.TenantImageWriter;
import java.util.UUID;

/** Stores reference images; phase 1 is local filesystem, later S3-compatible. */
public interface ImageStorage extends TenantImageReader, TenantImageWriter {
  String store(UUID tenantId, byte[] bytes);
}
