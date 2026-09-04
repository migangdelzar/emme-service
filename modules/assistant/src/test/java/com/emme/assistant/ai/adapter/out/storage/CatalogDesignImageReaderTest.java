package com.emme.assistant.ai.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emme.ai.contracts.image.TenantImageReader;
import com.emme.kernel.context.AiExecutionContext;
import com.emme.kernel.context.AiExecutionContextScope;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogDesignImageReaderTest {
  @Test
  void rejectsMissingExplicitContext() {
    var reader = new CatalogDesignImageReader(mock(TenantImageReader.class));
    assertThatThrownBy(() -> reader.read("images/design.jpg", null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void rejectsContextFromDifferentActiveTenant() {
    var reader = new CatalogDesignImageReader(mock(TenantImageReader.class));
    UUID activeTenant = UUID.randomUUID();
    UUID otherTenant = UUID.randomUUID();
    var active =
        new AiExecutionContext(
            activeTenant,
            UUID.randomUUID(),
            Set.of(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "trace-a",
            "idem-a");
    var supplied =
        new AiExecutionContext(
            otherTenant,
            UUID.randomUUID(),
            Set.of(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "trace-b",
            "idem-b");
    assertThatThrownBy(
            () -> AiExecutionContextScope.call(active, () -> reader.read("key", supplied)))
        .isInstanceOf(SecurityException.class);
  }

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

    var result = reader.read("images/design.jpg", context);

    assertThat(result).isPresent();
    verify(storage).read(tenant, "images/design.jpg");
    verify(storage, never()).read(argThat(other -> !tenant.equals(other)), anyString());
  }
}
