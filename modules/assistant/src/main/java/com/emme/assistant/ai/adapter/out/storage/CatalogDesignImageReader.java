package com.emme.assistant.ai.adapter.out.storage;

import com.emme.ai.contracts.image.TenantImageReader;
import com.emme.assistant.ai.application.port.out.DesignImageReader;
import com.emme.kernel.context.AiExecutionContext;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Reads images through the catalog port and never exposes filesystem locations. */
@Component
public class CatalogDesignImageReader implements DesignImageReader {
  private final TenantImageReader storage;

  public CatalogDesignImageReader(TenantImageReader storage) {
    this.storage = storage;
  }

  @Override
  public Optional<StoredImage> read(String storageKey, AiExecutionContext context) {
    if (context == null) throw new NullPointerException("context must not be null");
    var active = com.emme.kernel.context.AiExecutionContextScope.current();
    if (active.isPresent() && !active.orElseThrow().tenantId().equals(context.tenantId()))
      throw new SecurityException("image context tenant does not match active scope");
    return storage
        .read(context.tenantId(), storageKey)
        .map(image -> new StoredImage(image.bytes(), image.mediaType(), image.checksum()));
  }
}
