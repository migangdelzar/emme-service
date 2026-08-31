package com.emme.assistant.ai.adapter.in.web.controller;

import com.emme.ai.contracts.image.TenantImageWriter;
import com.emme.assistant.ai.adapter.in.web.request.DesignQuoteRequest;
import com.emme.assistant.ai.adapter.in.web.response.DesignQuoteResponse;
import com.emme.assistant.ai.adapter.in.web.security.AiWebExecutionContextFactory;
import com.emme.assistant.ai.api.command.ProcessDesignQuoteCommand;
import com.emme.assistant.ai.api.usecase.ProcessDesignQuoteUseCase;
import com.emme.assistant.ai.application.port.out.DesignImageMetadataRepository;
import com.emme.kernel.context.AiExecutionContextScope;
import com.emme.kernel.tracing.CorrelationId;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@ConditionalOnBean(ProcessDesignQuoteUseCase.class)
@RequestMapping(path = "/api/ai/quotes", version = "1.0")
public class DesignQuoteController {
  private static final long MAX_IMAGE_BYTES = 10 * 1024 * 1024;
  private final TenantImageWriter storage;
  private final ProcessDesignQuoteUseCase quote;
  private final AiWebExecutionContextFactory contexts;
  private final DesignImageMetadataRepository metadata;

  public DesignQuoteController(
      TenantImageWriter storage,
      ProcessDesignQuoteUseCase quote,
      AiWebExecutionContextFactory contexts,
      DesignImageMetadataRepository metadata) {
    this.storage = storage;
    this.quote = quote;
    this.contexts = contexts;
    this.metadata = metadata;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<DesignQuoteResponse> submit(
      @RequestPart("image") MultipartFile image,
      @RequestParam UUID conversationId,
      @RequestParam String templateKey,
      @RequestParam(required = false) String inputText,
      @RequestParam String idempotencyKey,
      @AuthenticationPrincipal Jwt jwt,
      Authentication authentication)
      throws Exception {
    if (image.isEmpty() || image.getSize() > MAX_IMAGE_BYTES)
      throw new IllegalArgumentException("image is empty or too large");
    if (image.getContentType() == null || !image.getContentType().startsWith("image/"))
      throw new IllegalArgumentException("image content type is not supported");
    String trace = Objects.requireNonNull(CorrelationId.get(), "Correlation ID is required");
    DesignQuoteRequest request = new DesignQuoteRequest(conversationId, templateKey, inputText);
    var context =
        contexts.forConversation(
            conversationId,
            trace,
            idempotencyKey,
            Objects.requireNonNull(jwt.getIssuer()).toString(),
            jwt.getSubject(),
            authentication.getAuthorities());
    return AiExecutionContextScope.call(
        context,
        () -> {
          String key = storage.store(context.tenantId(), image.getBytes());
          try {
            metadata.save(
                context.tenantId(),
                context.workflowId(),
                key,
                image.getContentType(),
                image.getSize());
            var result =
                quote.process(
                    new ProcessDesignQuoteCommand(request.templateKey(), request.inputText(), key));
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new DesignQuoteResponse(result.workflowId(), result.state()));
          } catch (RuntimeException | Error failure) {
            storage.delete(context.tenantId(), key);
            throw failure;
          }
        });
  }
}
