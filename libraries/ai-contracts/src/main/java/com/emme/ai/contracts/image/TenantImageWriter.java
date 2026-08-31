package com.emme.ai.contracts.image;

import java.util.UUID;

public interface TenantImageWriter {
  String store(UUID tenantId, byte[] bytes);
}
