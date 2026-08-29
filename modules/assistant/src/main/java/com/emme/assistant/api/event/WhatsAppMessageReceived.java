package com.emme.assistant.api.event;

import com.emme.assistant.api.command.ProcessWhatsAppMessageCommand;
import com.emme.kernel.context.TenantContextHolder;
import java.util.Objects;
import java.util.UUID;

/** Durable application event used to decouple webhook acknowledgement from AI processing. */
public record WhatsAppMessageReceived(ProcessWhatsAppMessageCommand command, UUID databaseId) {

  public WhatsAppMessageReceived(ProcessWhatsAppMessageCommand command) {
    this(command, TenantContextHolder.currentDatabaseOptional().orElse(null));
  }

  public WhatsAppMessageReceived {
    Objects.requireNonNull(command, "command must not be null");
  }
}
