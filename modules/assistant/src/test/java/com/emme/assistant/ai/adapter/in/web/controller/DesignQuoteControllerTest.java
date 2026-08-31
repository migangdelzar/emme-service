package com.emme.assistant.ai.adapter.in.web.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.emme.ai.contracts.image.TenantImageWriter;
import com.emme.assistant.ai.adapter.in.web.security.AiWebExecutionContextFactory;
import com.emme.assistant.ai.api.usecase.ProcessDesignQuoteUseCase;
import com.emme.assistant.ai.application.port.out.DesignImageMetadataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class DesignQuoteControllerTest {
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
