package com.emme.assistant.ai.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.emme.ai.contracts.image.TenantImageReader;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogDesignImageReaderTest {
  @Test
  void readsOnlyUsingAuthenticatedTenant() {
    TenantImageReader storage = mock(TenantImageReader.class);
    UUID tenant = UUID.randomUUID();
    when(storage.read(eq(tenant), eq("images/design.jpg")))
        .thenReturn(Optional.of(new TenantImageReader.StoredImage(new byte[] {1}, "image/jpeg")));
    var reader = new CatalogDesignImageReader(storage);
    var context =
        new AiExecutionContext(
            tenant,
            UUID.randomUUID(),
            Set.of(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "trace",
            "idem");

    var result = AiExecutionContextScope.call(context, () -> reader.read("images/design.jpg"));

    assertThat(result).isPresent();
    verify(storage).read(tenant, "images/design.jpg");
    verify(storage, never()).read(argThat(other -> !tenant.equals(other)), anyString());
  }
}
