package com.emme.assistant.ai.adapter.in.web.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.emme.ai.contracts.image.TenantImageWriter;
import com.emme.assistant.ai.adapter.in.web.security.AiWebExecutionContextFactory;
import com.emme.assistant.ai.api.usecase.ProcessDesignQuoteUseCase;
import com.emme.assistant.ai.application.port.out.DesignImageMetadataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class DesignQuoteControllerTest {
  @Test
  void deletesStoredImageWhenReadingUploadBytesFails() throws Exception {
    var storage = mock(TenantImageWriter.class);
    when(storage.store(any(), any())).thenReturn("tenant/image.img");
    var controller =
        new DesignQuoteController(
            storage,
            mock(ProcessDesignQuoteUseCase.class),
            new AiWebExecutionContextFactory(),
            mock(DesignImageMetadataRepository.class));
    var image = mock(org.springframework.web.multipart.MultipartFile.class);
    when(image.isEmpty()).thenReturn(false);
    when(image.getSize()).thenReturn(1L);
    when(image.getContentType()).thenReturn("image/jpeg");
    when(image.getBytes())
        .thenReturn(new byte[] {1})
        .thenThrow(new java.io.IOException("read failed"));
    var tenant = java.util.UUID.randomUUID();
    var jwt = mock(org.springframework.security.oauth2.jwt.Jwt.class);
    when(jwt.getIssuer()).thenReturn(new java.net.URL("https://issuer"));
    when(jwt.getSubject()).thenReturn("subject");
    var authentication = mock(org.springframework.security.core.Authentication.class);
    when(authentication.getAuthorities()).thenReturn(java.util.List.of());
    com.emme.kernel.context.TenantContextHolder.withTenantAndCorrelation(
        tenant,
        "trace",
        () ->
            assertThatThrownBy(
                    () ->
                        controller.submit(
                            image,
                            java.util.UUID.randomUUID(),
                            "base",
                            null,
                            "idem",
                            jwt,
                            authentication))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(java.io.IOException.class));
    verify(storage).delete(tenant, "tenant/image.img");
  }

  @Test
  void rejectsNonImageUploadBeforeStorageOrModelExecution() {
    var controller =
        new DesignQuoteController(
            mock(TenantImageWriter.class),
            mock(ProcessDesignQuoteUseCase.class),
            new AiWebExecutionContextFactory(),
            mock(DesignImageMetadataRepository.class));
    var file = new MockMultipartFile("image", "design.txt", "text/plain", new byte[] {1});

    assertThatThrownBy(
            () ->
                controller.submit(
                    file, java.util.UUID.randomUUID(), "base", null, "idem", null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("image content type is not supported");
  }
}
