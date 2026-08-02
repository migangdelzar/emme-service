package com.emme.assistant.api.usecase;

import com.emme.assistant.api.command.ProcessWhatsAppMessageCommand;

/** Processes one verified WhatsApp message through the Assistant workflow. */
public interface ProcessWhatsAppMessageUseCase {

  void process(ProcessWhatsAppMessageCommand command);
}
