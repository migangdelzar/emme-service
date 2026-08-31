package com.emme.assistant.ai.adapter.in.web.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.emme.ai.contracts.image.TenantImageWriter;
import com.emme.assistant.ai.adapter.in.web.security.AiWebExecutionContextFactory;
import com.emme.assistant.ai.api.result.QuoteWorkflowResult;
import com.emme.assistant.ai.api.usecase.ProcessDesignQuoteUseCase;
import com.emme.assistant.ai.application.port.out.DesignImageMetadataRepository;
import com.emme.assistant.ai.domain.workflow.QuoteWorkflowState;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class DesignQuoteControllerTest {
  @Test
  void initializesWorkflowBeforePersistingImageMetadataAndProcessingQuote() throws Exception {
    var storage = mock(TenantImageWriter.class);
    when(storage.store(any(), any())).thenReturn("tenant/image.img");
    var metadata = mock(DesignImageMetadataRepository.class);
    var quote = mock(ProcessDesignQuoteUseCase.class);
    var workflowId = java.util.UUID.randomUUID();
    when(quote.process(any()))
        .thenReturn(
            new QuoteWorkflowResult(
                workflowId,
                QuoteWorkflowState.QUOTE_READY,
                java.util.Optional.empty(),
                java.util.Optional.empty()));
    var controller =
        new DesignQuoteController(storage, quote, new AiWebExecutionContextFactory(), metadata);
    var image = new MockMultipartFile("image", "design.jpg", "image/jpeg", new byte[] {1});
    var tenant = java.util.UUID.randomUUID();
    var conversation = java.util.UUID.randomUUID();
    var jwt = mock(org.springframework.security.oauth2.jwt.Jwt.class);
    when(jwt.getIssuer()).thenReturn(new java.net.URL("https://issuer"));
    when(jwt.getSubject()).thenReturn("subject");
    var authentication = mock(org.springframework.security.core.Authentication.class);
    when(authentication.getAuthorities()).thenReturn(java.util.List.of());

    com.emme.kernel.context.TenantContextHolder.withTenantAndCorrelation(
        tenant,
        "trace",
        () -> controller.submit(image, conversation, "base", null, "idem", jwt, authentication));

    var order = inOrder(quote, metadata);
    order.verify(quote).initialize(any());
    order.verify(metadata).save(any(), any(), any(), any(), anyLong());
    order.verify(quote).process(any());
  }

  @Test
  void removesMetadataAndStorageWhenQuoteProcessingFails() throws Exception {
    var storage = mock(TenantImageWriter.class);
    when(storage.store(any(), any())).thenReturn("tenant/image.img");
    var metadata = mock(DesignImageMetadataRepository.class);
    var quote = mock(ProcessDesignQuoteUseCase.class);
    when(quote.process(any())).thenThrow(new IllegalStateException("downstream failed"));
    var controller =
        new DesignQuoteController(storage, quote, new AiWebExecutionContextFactory(), metadata);
    var image = new MockMultipartFile("image", "design.jpg", "image/jpeg", new byte[] {1});
    var tenant = java.util.UUID.randomUUID();
    var conversation = java.util.UUID.randomUUID();
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
                            image, conversation, "base", null, "idem", jwt, authentication))
                .isInstanceOf(IllegalStateException.class));

    var workflow =
        java.util.UUID.nameUUIDFromBytes(
            ("emme-ai-conversation-workflow-v1:" + conversation + ":idem")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    verify(metadata).delete(tenant, workflow, "tenant/image.img");
    verify(storage).delete(tenant, "tenant/image.img");
  }

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
                .isInstanceOf(java.io.UncheckedIOException.class)
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
