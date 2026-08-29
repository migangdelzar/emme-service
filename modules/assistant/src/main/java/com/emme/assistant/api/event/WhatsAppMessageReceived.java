package com.emme.assistant.api.event;

import com.emme.assistant.api.command.ProcessWhatsAppMessageCommand;
import java.util.Objects;

/** Durable application event used to decouple webhook acknowledgement from AI processing. */
public record WhatsAppMessageReceived(ProcessWhatsAppMessageCommand command) {

  public WhatsAppMessageReceived {
    Objects.requireNonNull(command, "command must not be null");
  }
}
