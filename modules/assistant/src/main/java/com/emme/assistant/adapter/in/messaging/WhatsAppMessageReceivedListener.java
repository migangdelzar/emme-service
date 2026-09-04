package com.emme.assistant.adapter.in.messaging;

import com.emme.assistant.api.event.WhatsAppMessageReceived;
import com.emme.assistant.api.usecase.ProcessWhatsAppMessageUseCase;
import com.emme.kernel.context.TenantContextHolder;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Receives durable WhatsApp events and restores their tenant execution context. */
@Component
@ConditionalOnBean(ProcessWhatsAppMessageUseCase.class)
public final class WhatsAppMessageReceivedListener {

  private final ProcessWhatsAppMessageUseCase useCase;

  public WhatsAppMessageReceivedListener(ProcessWhatsAppMessageUseCase useCase) {
    this.useCase = Objects.requireNonNull(useCase, "useCase must not be null");
  }

  @ApplicationModuleListener(id = "assistant.whatsapp-message-received")
  public void onMessage(WhatsAppMessageReceived event) {
    Objects.requireNonNull(event, "event must not be null");
    String traceId = "whatsapp:" + event.command().eventId();
    TenantContextHolder.withTenantAndCorrelation(
        event.command().tenantId(),
        event.databaseId(),
        traceId,
        () -> useCase.process(event.command()));
  }
}
